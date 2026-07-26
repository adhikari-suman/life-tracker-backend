# Life Tracker

A personal financial ledger. It records the flow of money between accounts using
double-entry bookkeeping: every financial event is captured as a set of entries whose debits
and credits balance, so what you earned and where it went are both recorded, not just what
survived into your bank.

## Language

**Transaction**:
A single financial event, recorded as a set of postings that balance: within one currency its
debits equal its credits exactly; when it spans two currencies each side holds its own real
amount and the two are equal in value at the rate recorded on the transaction. The unit of
recording — one payslip, one shop, one transfer is one transaction, however many postings it
takes to balance.
_Avoid_: entry, record, movement, purchase (a purchase is just one kind of transaction)

**Occurred At**:
When the money actually moved — the date on the receipt and the time on the clock beside it.
Read as a **wall clock**, never as an instant: 19:42 means 19:42 where you were standing, is
never converted into another zone, and so a late-evening purchase can never drift into the next
day. The date is what every summary groups by and the only part reporting consults; the time
orders a day and does nothing else. It is supplied by whoever records the transaction, not
observed by the system.
_Avoid_: timestamp, datetime (both name an instant on a universal clock, which this deliberately
is not), transaction date (that is only half of it)

**Recorded At**:
When a transaction was entered into the ledger, as against when the money moved. An audit fact
the ledger keeps for itself: it breaks ties in ordering and is never the answer to "when did this
happen". A transaction entered on Friday about Tuesday *occurred* on Tuesday and was *recorded*
on Friday, and collapsing the two is the mistake this pair of terms exists to prevent.
_Avoid_: created (it invites the created/updated/deleted trio, and this ledger has neither of the
other two — see the append-only note under Refund; the only correction is a reversing entry)

**Posting**:
One line of a transaction: an amount posted to a single account as either a debit or a
credit, carrying one label that says what the money was for. A transaction has two or more
postings, and its debits must equal its credits. A posting has no meaning on its own — it
exists only as part of the transaction that balances it.
_Avoid_: line, leg, split, entry

**Debit / Credit**:
The two directions a posting can take against its account. Which one raises a balance and
which lowers it depends on the account's normal side: a debit raises a debit-normal account
(a bank account, an expense) and lowers a credit-normal one (a credit card, income). The
everyday "money in / money out" a person thinks in is translated to the correct debit or
credit using the account's normal side, so the stored direction is always Dr/Cr.
_Avoid_: inflow/outflow, in/out (that is the human-facing view, not the stored direction)

**Account**:
A named place a balance lives, and the only thing that sits at either end of a posting.
Every account has a kind: **Asset** and **Liability** are accounts you own — your money and
your debts, the two that make up net worth; **Income** and **Expense** are the boundaries
where money enters and leaves your world; **Equity** is your capital — money that was already
yours when it first entered the ledger, neither earned nor owed. Accounts stay coarse — they exist to make transactions balance,
not to categorize spending; categorization is the job of labels. An account's kind fixes its
normal side — Asset and Expense grow on the debit side, Liability, Income and Equity on the
credit side — so a posting reads as a rise or fall in the balance with no further setup.
Every account also has a single home currency; its postings are always in that currency, and
a transaction that spans two currencies carries the rate that ties them together.
_Avoid_: bucket, wallet, ledger, category (a category is a label, not an account)

**Internal Transfer**:
A transaction that moves your own money between two accounts you hold — Asset to Asset, like
bank to cash — so nothing entered or left your world. It touches no Income or Expense account
and therefore never counts as earning or spending. Marked with an `ITR` tag so it is trivial
to filter out. Getting this right — not double-counting a moved £200 as both income and
expense — is the core thing this ledger exists to fix, and the thing other apps get wrong.
Paying down a debt is its Asset↔Liability cousin; see Payment.
_Avoid_: transfer (money crossing to someone else's account is not internal)

**Payment**:
Settling a debt — paying down a credit card or a loan — recorded as a debit to the liability
and a credit to whatever it is paid from. Like an internal transfer it touches no Income or
Expense account, so it never counts as spending: the spending was already recognized when the
debt was incurred, the moment a card is swiped, not when the bill is later paid.
_Avoid_: expense (paying a card bill is not spending — the swipe was)

**Refund**:
Money coming back on an earlier purchase, recorded as a new transaction that credits the same
expense account and label the purchase debited — never as income. It is negative spending
against its own category, so the net spent falls. Booking it as income instead would leave the
category overstated and invent earnings that never happened. The original purchase is never
edited (the ledger is append-only); a refund landing in a later month can make that month's
category read negative, which is correct.
_Avoid_: income, adjustment

**Opening Balance**:
The balance an account already carries the day it enters the ledger — an existing £3,000 in
the bank, an existing £500 owed on a card. Recorded as an ordinary balanced transaction whose
other side is Equity, so it counts as neither income nor a transfer: you walked in with this
money, you did not earn it. There is deliberately no period close and no retained earnings —
Equity holds only this starting capital, and net worth is read directly as Assets minus
Liabilities.
_Avoid_: starting balance, initial deposit

**Exchange Rate**:
The ratio between the two currencies of a cross-currency transaction, derived from its two
real amounts (an £80 leg and a $100 leg imply 1.25) and recorded on the transaction for
reference. It is never used to compute an amount — both amounts come from real statements — so
there is no rounding to reconcile.
_Avoid_: FX rate, conversion factor

**Merchant**:
An organization you transacted with but hold no running balance against — a shop you buy
from, a service you pay, an employer who pays you. Recorded as a reference on the transaction
purely so you can slice spending by who or where (all spend at Walmart, across its branches),
never as an account and never a participant in the balance. A counterparty you *do* hold a
balance with is not a merchant — it is a Receivable account, which names that person itself.
_Avoid_: party, entity, vendor, payee

**Receivable**:
A per-person Asset account holding the running balance between you and someone you lend to or
borrow from — the account names them, so no separate "party" is needed. A positive balance
means they owe you — they are your debtor; it may go negative, meaning you
owe them — they are your creditor. Lending is Asset↔Asset and therefore P&L-neutral: net worth
is unchanged and it is not spending — the money has merely become a claim. Only the balance is
signed; every posting is still a non-negative amount.
_Avoid_: loan, IOU (those name a use of it; debtor/creditor name its two states)

**Branch**:
A specific location of a Merchant, recording where a transaction physically happened. Optional
and often absent — an online order or a subscription has no branch, and that absence is a
fact about the event, not missing data. Like the Merchant, a reference for analysis, not an
account.
_Avoid_: store, outlet, location

**Label**:
A user-defined tag on a posting that says what the money was for, so you can summarize your
own spending however you like — `fuel`, `groceries`, `fast food`, `salary`. A posting carries
at most one, and only a posting recording money entering or leaving your world can carry one
at all: an internal transfer, a payment, an opening balance has nothing to categorize and so
takes none. Tagging is optional — what is left untagged is Uncategorized, not wrong. Labels
form a parent-child tree (`food` → `fast food`), at most three levels deep, with no cycles,
and any of them may be used directly — tag a posting `food` when you don't know or don't care
which kind. A label rolls up into its ancestors, so tagging a posting `fast food` also counts
it toward `food` in any summary; the roll-up follows the tree as it stands now, so
reorganizing the tree reorganizes every summary, past months included. A label may be renamed,
moved, or retired from use without disturbing what it was already applied to, and what a
posting is tagged with can be changed at any time however old it is — a label describes money,
it never moves it. Labels are the whole of categorization; accounts do not categorize.
_Avoid_: category, tag, type — a label says what the money was *for*, never what kind of
transaction it was; "internal transfer", "opening balance" and "credit-card payment" are facts
about the accounts a transaction touches, not labels

**Uncategorized**:
Where money with no label lands in a summary — not a label anyone creates or applies, but the
name for what remains once every labelled posting has been counted. It exists so a summary
always accounts for everything: the labelled parts plus Uncategorized are the whole. It cannot
be renamed, nested, or tagged onto anything, and it vanishes the moment you tag what was in it.
_Avoid_: unlabelled, misc, other (those are labels you might create; this is the absence of one)
