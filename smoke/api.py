#!/usr/bin/env python3
"""
Smoke test: the whole HTTP surface against a RUNNING stack.

This is not a replacement for `./gradlew test` — those tests own the units and the persistence.
This exercises the assembled thing over the wire and asserts the CONTRACT and the DOMAIN RULES:
that the API matches life-tracker-contracts/openapi.yaml, and that the ledger behaves the way
CONTEXT.md and docs/adr say it does.

It earns its place because it catches a class of bug the suite cannot. Two live examples, both
found while the whole suite was green:

  - `time` came back as "19:42:00", violating the spec's own
    pattern ^([01][0-9]|2[0-3]):[0-5][0-9]$ — nothing in Java compared the response to the spec.
  - omitting `time` NPE'd into a 500 instead of a 422, because the DTO had no @NotNull.

Every check names the rule it defends, not just the endpoint it calls. A failure should tell you
which decision broke, not merely that a number moved.

Usage:
    smoke/run.sh                 # both suites, the normal way in
    python3 smoke/api.py         # this one alone

Environment:
    API_BASE_URL   default http://localhost:8080/v1
    APP_CONTAINER  default life-tracker-backend-app-1 (read for the stub email tokens)

Safe to run repeatedly: it registers a fresh user per run and asserts only within that Book, so
totals stay deterministic without needing an empty database.
"""
import json
import os
import re
import subprocess
import sys
import urllib.error
import urllib.request
from datetime import datetime
from decimal import Decimal

API = os.environ.get("API_BASE_URL", "http://localhost:8080/v1")
APP_CONTAINER = os.environ.get("APP_CONTAINER", "life-tracker-backend-app-1")
PASSWORD = "correct horse battery"

PASS = FAIL = 0
FAILURES = []


def call(method, path, body=None, token=None):
    req = urllib.request.Request(API + path, method=method)
    req.add_header("content-type", "application/json")
    if token:
        req.add_header("authorization", "Bearer " + token)
    data = json.dumps(body).encode() if body is not None else None
    try:
        with urllib.request.urlopen(req, data) as r:
            raw = r.read().decode()
            return r.status, (json.loads(raw) if raw.strip() else None)
    except urllib.error.HTTPError as e:
        raw = e.read().decode()
        try:
            return e.code, json.loads(raw) if raw.strip() else None
        except json.JSONDecodeError:
            return e.code, raw
    except urllib.error.URLError as e:
        sys.exit(f"cannot reach {API} ({e.reason}).\nStart the stack first:  docker compose up -d --build --scale app=1")


def check(label, expected, actual):
    global PASS, FAIL
    if expected == actual:
        PASS += 1
        print(f"  ok   {label}")
    else:
        FAIL += 1
        FAILURES.append(label)
        print(f"  FAIL {label}\n         expected: {expected!r}\n         actual:   {actual!r}")


def ok(label, cond, detail=""):
    check(label + (f" ({detail})" if detail else ""), True, bool(cond))


def section(name):
    print(f"\n=== {name} ===")


def must(status, expected, what):
    if status != expected:
        sys.exit(f"setup failed: {what} returned {status}, expected {expected}")


# ----------------------------------------------------------------------------------- auth
section("auth & sessions")
stamp = datetime.now().strftime("%H%M%S%f")
email = f"smoke-{stamp}@example.com"

st, reg = call("POST", "/auth/register", {"email": email, "password": PASSWORD})
check("POST /auth/register -> 201", 201, st)
must(st, 201, "register")
tok, refresh = reg["accessToken"], reg["refreshToken"]

check("duplicate email -> 409", 409,
      call("POST", "/auth/register", {"email": email, "password": PASSWORD})[0])
check("malformed email -> 422", 422,
      call("POST", "/auth/register", {"email": "bad", "password": PASSWORD})[0])
check("password under policy -> 422", 422,
      call("POST", "/auth/register", {"email": f"x{stamp}@e.com", "password": "short"})[0])

check("POST /auth/login -> 200", 200,
      call("POST", "/auth/login", {"email": email, "password": PASSWORD})[0])
check("wrong password -> 401", 401,
      call("POST", "/auth/login", {"email": email, "password": "wrong password here"})[0])

st, me = call("GET", "/me", token=tok)
check("GET /me -> 200", 200, st)
check("  /me reports the registered email", email, me.get("email"))

st, sessions = call("GET", "/auth/sessions", token=tok)
check("GET /auth/sessions -> 200", 200, st)
# Counted BEFORE the reuse test below: detecting a replayed refresh token revokes that whole
# session lineage (ADR-0007), so testing reuse first would destroy the session being counted.
ok("register and login are two distinct Sessions", len(sessions) >= 2, f"{len(sessions)}")

st, rot = call("POST", "/auth/refresh", {"refreshToken": refresh})
check("POST /auth/refresh -> 200", 200, st)
ok("the refresh token ROTATES", rot["refreshToken"] != refresh)

st, _ = call("POST", "/auth/refresh", {"refreshToken": refresh})
ok("replaying the old refresh token is refused", st in (401, 404), f"{st}")

st, after = call("GET", "/auth/sessions", token=tok)
ok("...and reuse REVOKED that session lineage", len(after) < len(sessions),
   f"{len(sessions)} -> {len(after)}")

# ------------------------------------------------------------------------------- accounts
section("accounts — all five kinds (ADR-0001)")


def mk_account(name, kind, currency="USD", t=None):
    st, a = call("POST", "/accounts", {"name": name, "kind": kind, "currency": currency}, token=t or tok)
    must(st, 201, f"create account {name}")
    return a["id"]


bank = mk_account("Bank", "ASSET")
card = mk_account("Card", "LIABILITY")
groceries = mk_account("Groceries", "EXPENSE")
restaurants = mk_account("Restaurants", "EXPENSE")
salary = mk_account("Salary", "INCOME")
equity = mk_account("Opening Balances", "EQUITY")
eur = mk_account("Euro account", "ASSET", "EUR")
friend = mk_account("Alex", "ASSET")  # a Receivable — an Asset that names the person

st, accounts = call("GET", "/accounts", token=tok)
check("all five kinds accepted; 8 accounts exist", 8, len(accounts))
ok("a new account opens at zero", all(Decimal(a["balance"]["amount"]) == 0 for a in accounts))
check("an invented account kind -> 422", 422,
      call("POST", "/accounts", {"name": "Nope", "kind": "WALLET", "currency": "USD"}, token=tok)[0])

# --------------------------------------------------------------------------- transactions
section("transactions — the movement shapes (ADR-0012)")


def move(date, time, frm, to, amount, currency="USD", to_amount=None, label=None, t=None):
    body = {"date": date, "time": time, "from": frm, "to": to,
            "amount": {"amount": amount, "currency": currency}}
    if to_amount:
        body["toAmount"] = to_amount
    if label:
        body["labelId"] = label
    return call("POST", "/transactions", body, token=t or tok)


check("opening balance (Equity -> Asset) -> 201", 201,
      move("2026-07-01", "09:00", equity, bank, "3000.00")[0])
# A debt you already owe must CREDIT the card — Liability is credit-normal (ADR-0001), so the
# movement runs card -> equity. Sending equity -> card debits it and opens the book at MINUS 500.
check("opening balance on a LIABILITY (card -> Equity) -> 201", 201,
      move("2026-07-01", "09:05", card, equity, "500.00")[0])
check("spend (Asset -> Expense) -> 201", 201,
      move("2026-07-02", "12:30", bank, groceries, "80.00")[0])
check("spend on a CARD (Liability -> Expense) -> 201", 201,
      move("2026-07-02", "19:42", card, restaurants, "45.00")[0])
check("income (Income -> Asset) -> 201", 201,
      move("2026-07-03", "08:00", salary, bank, "2400.00")[0])
check("from == to is refused -> 422", 422,
      move("2026-07-04", "10:00", bank, bank, "10.00")[0])
check("cross-currency WITHOUT toAmount -> 422", 422,
      move("2026-07-05", "11:00", bank, eur, "100.00")[0])
check("cross-currency carries two real amounts -> 201", 201,
      move("2026-07-05", "11:00", bank, eur, "100.00",
           to_amount={"amount": "90.00", "currency": "EUR"})[0])

# Transfers, debt payments and loans are all P&L-neutral: none touches Income or Expense.
savings = mk_account("Savings", "ASSET")
check("internal transfer (Asset -> Asset) -> 201", 201,
      move("2026-07-06", "13:00", bank, savings, "200.00")[0])
check("debt payment (Asset -> Liability) -> 201", 201,
      move("2026-07-07", "14:00", bank, card, "300.00")[0])
check("lending to a Receivable (Asset -> Asset) -> 201", 201,
      move("2026-07-08", "15:00", bank, friend, "50.00")[0])

# additionalProperties: false, everywhere. A typo'd field is an error, not a no-op.
st, _ = call("POST", "/transactions",
             {"date": "2026-07-08", "time": "15:00", "from": bank, "to": groceries,
              "amount": {"amount": "1.00", "currency": "USD"}, "nonsense": 1}, token=tok)
check("an undeclared field -> 400 (strict bodies)", 400, st)

# ------------------------------------------------------------- required fields, everywhere
section("a missing required field is 422 VALIDATION on EVERY endpoint")
# The spec states this as a BLANKET rule, so it has to hold across the surface, not just where
# someone remembered @Valid. It did not: /accounts, /labels and the posting-label sub-resource
# each 500'd on a null that reached the domain, while /accounts missing `kind` happened to answer
# 422 because the enum failed to deserialize — the same client mistake getting different answers
# depending on which field it landed on.
for label, method, path, body in [
    ("POST /accounts without name", "POST", "/accounts", {"kind": "ASSET", "currency": "USD"}),
    ("POST /accounts without kind", "POST", "/accounts", {"name": "X", "currency": "USD"}),
    ("POST /accounts without currency", "POST", "/accounts", {"name": "X", "kind": "ASSET"}),
    ("POST /labels without name", "POST", "/labels", {}),
    ("PUT  /postings/{id}/label without labelId", "PUT",
     f"/postings/{'0'*8}-0000-4000-8000-{'0'*12}/label", {}),
]:
    st, problem = call(method, path, body, token=tok)
    check(f"{label} -> 422", 422, st)

# Two operations document their OWN answer and are deliberate, not oversights: a credential that
# is absent is as invalid as one that is malformed, and password reset must not vary its response
# by whether the address exists.
check("verify-email without a token -> 400 INVALID_TOKEN (documented)", 400,
      call("POST", "/auth/verify-email", {})[0])
check("password-reset without an email -> 202 (never enumerates)", 202,
      call("POST", "/auth/password-reset", {})[0])

# -------------------------------------------------------------------------------- labels
section("labels — the tree (ADR-0015)")


def mk_label(name, parent=None):
    body = {"name": name}
    if parent:
        body["parentId"] = parent
    return call("POST", "/labels", body, token=tok)


st, food = mk_label("food")
check("create a root label -> 201", 201, st)
st, fastfood = mk_label("fast food", food["id"])
check("create a child -> 201", 201, st)
st, burgers = mk_label("burgers", fastfood["id"])
check("create a grandchild (3 levels) -> 201", 201, st)
check("a FOURTH level is refused -> 422", 422, mk_label("too deep", burgers["id"])[0])
check("a duplicate sibling name (case-insensitive) -> 422", 422, mk_label("Food")[0])
check("path is built from the root", "food / fast food / burgers", burgers["path"])

# Tested while the chain is still intact: food -> fast food -> burgers. Re-parenting food under
# its own grandchild would make it its own ancestor. Do this BEFORE moving fast food to the root,
# which severs the ancestry and makes the very same call perfectly legal.
check("a cycle is refused -> 422", 422,
      call("PATCH", "/labels/" + food["id"], {"parentId": burgers["id"]}, token=tok)[0])

ok("rename works",
   call("PATCH", "/labels/" + burgers["id"], {"name": "smash burgers"}, token=tok)[0] in (200, 204))
ok("explicit null moves a label to the root",
   call("PATCH", "/labels/" + fastfood["id"], {"parentId": None}, token=tok)[0] in (200, 204))

# ------------------------------------------------------------------- posting labels
section("posting labels — metadata, not ledger truth (ADR-0014)")
st, txns = call("GET", "/transactions", token=tok)
check("GET /transactions -> 200", 200, st)

spend_txn = next(t for t in txns if t["date"] == "2026-07-02" and t["time"] == "12:30")
expense_leg = next(p for p in spend_txn["postings"] if p["direction"] == "DEBIT")
asset_leg = next(p for p in spend_txn["postings"] if p["direction"] == "CREDIT")

check("label an EXPENSE posting -> 204", 204,
      call("PUT", f"/postings/{expense_leg['id']}/label", {"labelId": food["id"]}, token=tok)[0])
check("labelling the ASSET leg is refused -> 422", 422,
      call("PUT", f"/postings/{asset_leg['id']}/label", {"labelId": food["id"]}, token=tok)[0])
check("an unknown label -> 404", 404,
      call("PUT", f"/postings/{expense_leg['id']}/label",
           {"labelId": "00000000-0000-4000-8000-000000000000"}, token=tok)[0])
check("clearing a posting's label -> 204", 204,
      call("DELETE", f"/postings/{expense_leg['id']}/label", token=tok)[0])
check("re-applying it -> 204", 204,
      call("PUT", f"/postings/{expense_leg['id']}/label", {"labelId": food["id"]}, token=tok)[0])

# ------------------------------------------------------------------------------- reports
section("reports — computed, per currency (ADR-0004, ADR-0013)")


def usd_of(totals):
    """CurrencyAmount.amount is a Money object, not a bare string."""
    return next(Decimal(t["amount"]["amount"]) for t in totals if t["currency"] == "USD")


st, nw = call("GET", "/reports/net-worth", token=tok)
check("GET /reports/net-worth -> 200", 200, st)
usd = next(c for c in nw["byCurrency"] if c["currency"] == "USD")
# Assets:  bank 3000 -80 +2400 -100 -200 -300 -50 = 4670;  savings 200;  Alex 50  => 4920
# Liabilities: card 500 +45 -300 = 245
check("USD assets", Decimal("4920.0000"), Decimal(usd["assets"]["amount"]))
check("USD liabilities", Decimal("245.0000"), Decimal(usd["liabilities"]["amount"]))
check("USD net worth = assets - liabilities", Decimal("4675.0000"), Decimal(usd["netWorth"]["amount"]))
ok("EUR is reported SEPARATELY, never summed across currencies",
   any(c["currency"] == "EUR" for c in nw["byCurrency"]))

st, spend = call("GET", "/reports/spending", token=tok)
check("GET /reports/spending -> 200", 200, st)
usd_total = usd_of(spend["totals"])
# Only the two real expenses: 80 groceries + 45 restaurants. The transfer, the card payment and
# the loan touch no Expense account, so they are excluded BY CONSTRUCTION — not by a filter.
check("spending excludes transfers/payments/loans by construction", Decimal("125.0000"), usd_total)
check("byAccount reconciles to totals", usd_total,
      sum(Decimal(a["amount"]["amount"]) for a in spend["byAccount"] if a["currency"] == "USD"))
# The spec calls this "the check that catches a roll-up bug": summing `own` (never `rolledUp`,
# which double-counts through ancestors) must reproduce the per-currency total.
check("summing byLabel.own reconciles to totals (the roll-up check)", usd_total,
      sum(Decimal(l["own"]["amount"]) for l in spend["byLabel"] if l["currency"] == "USD"))
ok("an Uncategorized row exists for the untagged 45.00",
   any(l["labelId"] is None for l in spend["byLabel"]))

st, income = call("GET", "/reports/income", token=tok)
check("GET /reports/income -> 200", 200, st)
check("income counts only Income accounts (the 2400 salary)",
      Decimal("2400.0000"), usd_of(income["totals"]))
check("a one-day range groups by DATE and ignores the time", Decimal("125.0000"),
      usd_of(call("GET", "/reports/spending?from=2026-07-02&to=2026-07-02", token=tok)[1]["totals"]))
check("a range with no activity totals nothing", [],
      call("GET", "/reports/spending?from=2020-01-01&to=2020-01-02", token=tok)[1]["totals"])

# ------------------------------------------------------------------------------ ADR-0018
section("ADR-0018 — the wall clock")
move("2026-07-09", "08:15", bank, groceries, "3.20")
move("2026-07-09", "22:05", bank, groceries, "9.99")
st, late = move("2026-07-09", "19:42", bank, groceries, "1.10")
check("time round-trips as HH:mm, never with seconds", "19:42", late["time"])

st, txns = call("GET", "/transactions", token=tok)
check("within a day, newest first by time", ["22:05", "19:42", "08:15"],
      [t["time"] for t in txns if t["date"] == "2026-07-09"])

st, problem = call("POST", "/transactions",
                   {"date": "2026-07-09", "from": bank, "to": groceries,
                    "amount": {"amount": "1.00", "currency": "USD"}}, token=tok)
# 422, not 400: the body PARSES, it just omits a required field. 400 is reserved for a body that
# cannot be read as written. And never 500 — that was an unguarded null reaching the use case.
check("a missing time is refused (422, not 500)", 422, st)
check("  ...as VALIDATION", "VALIDATION", (problem or {}).get("code"))
check("a time carrying an OFFSET is refused (400)", 400,
      call("POST", "/transactions",
           {"date": "2026-07-09", "time": "19:42:00+05:00", "from": bank, "to": groceries,
            "amount": {"amount": "1.00", "currency": "USD"}}, token=tok)[0])

# ------------------------------------------------------------------------------- sharing
section("sharing (ADR-0008) — gated on a verified email")
# Verification gates exactly one thing today: sharing. Prove the refusal before the happy path.
st, problem = call("POST", "/me/share-link", token=tok)
check("an unverified user cannot share -> 403", 403, st)
check("  ...with EMAIL_NOT_VERIFIED", "EMAIL_NOT_VERIFIED", (problem or {}).get("code"))


def verify(addr):
    """Consume the single-use token LoggingEmailSender printed for `addr`.

    Reads BOTH streams: the container logs to stdout, but docker splits the two and reading
    only one silently finds nothing.
    """
    p = subprocess.run(["docker", "logs", APP_CONTAINER], capture_output=True, text=True)
    tokens = re.findall(rf"verify {re.escape(addr)} with token (\S+)", p.stdout + p.stderr)
    if not tokens:
        return None
    return call("POST", "/auth/verify-email", {"token": tokens[-1]})[0]


check("consuming the emailed verification token -> 204", 204, verify(email))
ok("a VERIFIED user can create a share link",
   call("POST", "/me/share-link", token=tok)[0] in (200, 201))
check("GET /me/share-link -> 200 once sharing is on", 200,
      call("GET", "/me/share-link", token=tok)[0])
ok("revoking the share link", call("DELETE", "/me/share-link", token=tok)[0] in (200, 204))
check("...after which link sharing is off -> 404", 404,
      call("GET", "/me/share-link", token=tok)[0])

other_email = f"other-{stamp}@example.com"
st, other = call("POST", "/auth/register", {"email": other_email, "password": PASSWORD})
must(st, 201, "register second user")
other_tok = other["accessToken"]
verify(other_email)

ok("granting view to a named, registered User",
   call("POST", "/me/view-grants", {"email": email}, token=other_tok)[0] in (200, 201))
ok("granting to YOURSELF is refused",
   call("POST", "/me/view-grants", {"email": other_email}, token=other_tok)[0] in (403, 422))
check("granting to an unregistered address -> 404", 404,
      call("POST", "/me/view-grants", {"email": "nobody@nowhere.example"}, token=other_tok)[0])
check("the grant is listed", 1, len(call("GET", "/me/view-grants", token=other_tok)[1]))

# ----------------------------------------------------------------------------- isolation
section("tenant isolation (ADR-0006)")
check("a second user's Book is empty", 0, len(call("GET", "/accounts", token=other_tok)[1]))
check("...and sees none of the first user's transactions", 0,
      len(call("GET", "/transactions", token=other_tok)[1]))
check("fetching another Book's transaction by id -> 404", 404,
      call("GET", f"/transactions/{spend_txn['id']}", token=other_tok)[0])
check("unauthenticated -> 401", 401, call("GET", "/transactions")[0])
check("JWKS is public -> 200", 200, call("GET", "/.well-known/jwks.json")[0])

# -----------------------------------------------------------------------------------------
print("\n" + "=" * 46)
print(f"  api:  passed {PASS}   failed {FAIL}")
if FAILURES:
    print("\n  failing checks:")
    for f in FAILURES:
        print(f"    - {f}")
print("=" * 46)
sys.exit(1 if FAIL else 0)
