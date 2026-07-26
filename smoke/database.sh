#!/usr/bin/env bash
# Smoke test: the database itself.
#
# Everything here is invisible to the HTTP surface. A missing grant, a nullable money column or a
# float where a NUMERIC belongs passes every API test right up until the day it doesn't — and the
# two-role split (ADR-0016) is precisely the kind of guarantee that quietly stops holding when a
# changeset forgets something.
#
# The integrity checks run against STORED ROWS rather than through the API that wrote them, so
# they would catch a write path that bypassed the aggregate.
#
# Usage:
#     smoke/run.sh              # both suites
#     smoke/database.sh         # this one alone
#
# Environment:
#     DB_CONTAINER  default life-tracker-backend-db-1
#     DB_NAME       default lifetracker
set -uo pipefail

C=${DB_CONTAINER:-life-tracker-backend-db-1}
DB=${DB_NAME:-lifetracker}
PASS=0; FAIL=0

ok()  { PASS=$((PASS+1)); printf '  ok   %s\n' "$1"; }
bad() { FAIL=$((FAIL+1)); printf '  FAIL %s\n         %s\n' "$1" "$2"; }
mig() { docker exec "$C" psql -U lifetracker_migrator -d "$DB" -tAc "$1" 2>&1; }
app() { docker exec "$C" psql -U lifetracker_app      -d "$DB" -tAc "$1" 2>&1; }

if ! docker exec "$C" true 2>/dev/null; then
  echo "cannot reach container '$C'."
  echo "Start the stack first:  docker compose up -d --build --scale app=1"
  exit 1
fi

echo "=== the two-role split (ADR-0016) ==="
for r in lifetracker_migrator lifetracker_app; do
  if [ "$(mig "select count(*) from pg_roles where rolname='$r';")" = 1 ]
    then ok "$r exists"; else bad "$r exists" "missing"; fi
done
if [ "$(mig "select count(*) from pg_roles where rolname like 'lifetracker%' and rolsuper;")" = 0 ]
  then ok "neither role is a superuser"; else bad "neither role is a superuser" "one is"; fi

# The point of the split: the application cannot reshape the schema even if Hibernate wanted to.
denied() { # $1 = label, $2 = sql
  local out; out=$(app "$2")
  if echo "$out" | grep -qi "permission denied\|must be owner"
    then ok "$1"; else bad "$1" "$out"; fi
}
denied "app role CANNOT create a table" "create table should_not_exist(id int);"
denied "app role CANNOT alter a table"  "alter table transactions add column sneaky int;"
denied "app role CANNOT drop a table"   "drop table postings;"

echo
echo "=== the app role can still do its job on EVERY table ==="
# Checked exhaustively, not spot-checked: one table missing one verb is exactly the failure
# ALTER DEFAULT PRIVILEGES exists to prevent, and it would be invisible until that code path ran.
MISSING=""; N=0
for t in $(mig "select tablename from pg_tables where schemaname='public' and tablename not like 'databasechange%' order by 1;"); do
  N=$((N+1))
  for p in SELECT INSERT UPDATE DELETE; do
    [ "$(mig "select has_table_privilege('lifetracker_app','$t','$p');")" = t ] || MISSING="$MISSING $t/$p"
  done
done
if [ -z "$MISSING" ]
  then ok "SELECT/INSERT/UPDATE/DELETE on all $N tables"
  else bad "app role has full DML on every table" "missing:$MISSING"; fi

echo
echo "=== the column types the ledger rests on ==="
notnull() {
  if [ "$(mig "select attnotnull from pg_attribute where attrelid='$1'::regclass and attname='$2';")" = t ]
    then ok "$1.$2 is NOT NULL"; else bad "$1.$2 is NOT NULL" "nullable"; fi
}
notnull transactions tx_date
notnull transactions tx_time
notnull transactions owner_id
notnull postings amount
notnull postings currency
notnull accounts owner_id

typeis() {
  local got; got=$(mig "select format_type(atttypid,atttypmod) from pg_attribute where attrelid='$1'::regclass and attname='$2';")
  if [ "$got" = "$3" ]; then ok "$1.$2 is $3"; else bad "$1.$2 is $3" "got $got"; fi
}
# Money is NUMERIC(19,4). Never float, never Postgres `money` (locale-dependent rounding).
typeis postings amount "numeric(19,4)"
# Zoneless, never timetz: an offset is exactly what a wall clock must not carry (ADR-0018).
typeis transactions tx_time "time without time zone"

# Reported rather than asserted. The schema is code-first from the JPA entities (ADR-0009), so an
# entity holding a raw UUID instead of a mapped association simply produces no FK. Worth SEEING:
# it says which links the database will defend and which rest entirely on the aggregate.
echo "  --   foreign keys present:"
mig "select '       '||conrelid::regclass||'.'||a.attname||' -> '||confrelid::regclass
     from pg_constraint c join pg_attribute a on a.attrelid=c.conrelid and a.attnum=c.conkey[1]
     where contype='f' order by 1;"

echo
echo "=== double-entry, checked against stored rows ==="
# SAME-currency: debits equal credits, exactly. Cross-currency is excluded here because it is NOT
# meant to balance within one currency — ADR-0002 gives each side its own real amount and ties
# them with a recorded rate, so a per-currency zero sum would flag correct data as broken.
UNBAL=$(mig "select count(*) from (
  select t.id from transactions t join postings p on p.transaction_id = t.id
  where t.exchange_rate is null
  group by t.id, p.currency
  having sum(case when p.side='DEBIT' then p.amount else -p.amount end) <> 0
) x;")
if [ "$UNBAL" = 0 ]
  then ok "every same-currency transaction balances exactly"
  else bad "same-currency transactions balance" "$UNBAL unbalanced"; fi

# CROSS-currency: two legs, two currencies, opposite sides, and the stored rate genuinely relates
# the two real amounts (rate = to ÷ from). This only checks self-consistency — the rate is never
# multiplied to MAKE an amount.
XBAD=$(mig "select count(*) from (
  select t.id from transactions t join postings p on p.transaction_id = t.id
  where t.exchange_rate is not null
  group by t.id, t.exchange_rate
  having count(*) <> 2
      or count(distinct p.currency) <> 2
      or count(distinct p.side) <> 2
      or abs(max(case when p.side='DEBIT'  then p.amount end)
           - max(case when p.side='CREDIT' then p.amount end) * t.exchange_rate) > 0.0001
) x;")
XN=$(mig "select count(*) from transactions where exchange_rate is not null;")
if [ "$XBAD" = 0 ]
  then ok "every cross-currency transaction agrees with its recorded rate ($XN checked)"
  else bad "cross-currency rate consistency" "$XBAD inconsistent"; fi

zero() { # $1 = label, $2 = sql returning a count that must be 0
  local n; n=$(mig "$2")
  if [ "$n" = 0 ]; then ok "$1"; else bad "$1" "$n"; fi
}
zero "no orphaned postings" \
  "select count(*) from postings p left join transactions t on t.id=p.transaction_id where t.id is null;"
zero "every transaction has at least two postings" \
  "select count(*) from (select transaction_id from postings group by transaction_id having count(*) < 2) x;"
zero "every posting amount is non-negative (only a BALANCE may be signed)" \
  "select count(*) from postings where amount < 0;"

echo
echo "=== label tree integrity (ADR-0015) ==="
DEEP=$(mig "with recursive d as (
  select id, parent_id, 1 lvl from labels where parent_id is null
  union all select l.id, l.parent_id, d.lvl+1 from labels l join d on l.parent_id = d.id
) select coalesce(max(lvl),0) from d;")
if [ "$DEEP" -le 3 ]
  then ok "no label is deeper than three levels (max $DEEP)"
  else bad "label tree is at most 3 deep" "max $DEEP"; fi
zero "no label is its own parent" "select count(*) from labels where parent_id = id;"

echo
echo "=== migrations ==="
# Counts changeSet BLOCKS, not include lines: one file may declare several (010-create-labels
# holds two), and the master itself declares the 000-baseline tag. Comparing against include
# lines undercounts and reports a healthy database as broken.
CL=migrations/src/main/resources/db/changelog
CS=$(mig "select count(*) from databasechangelog;")
DECLARED=$(cat "$CL/db.changelog-master.yaml" "$CL"/changes/*.yaml 2>/dev/null | grep -c '^\s*-\s*changeSet:')
if [ "$CS" = "$DECLARED" ]
  then ok "every declared changeset is applied ($CS)"
  else bad "every declared changeset is applied" "applied $CS, declared $DECLARED"; fi
zero "none re-ran or failed a checksum" \
  "select count(*) from databasechangelog where exectype <> 'EXECUTED';"

echo
echo "=============================================="
echo "  database:  passed $PASS   failed $FAIL"
echo "=============================================="
[ "$FAIL" -eq 0 ]
