# Identity & Sharing

The context around the ledger that answers one question: *who is this, and whose Book may they
touch?* It owns Users, authentication, and the read-only sharing of a Book. It records no money
movement — that is the Ledger's job; this context only decides who may read or write it.

## Language

**User**:
An authenticated person, identified by credentials they prove at login. The single kind of
identity in the system — everyone who signs in is a User. "Owner" and "viewer" are not kinds of
User; they are relationships a User has to a Book, so the same identity that views your Book can
own their own.
_Avoid_: account (that word belongs to the Ledger — a place a balance lives), member, principal

**Session**:
A User's standing login on one device — the durable "you are signed in here," as opposed to the
single act of logging in. Independently revocable: signing out ends this Session; "sign out
everywhere" ends all of a User's at once. One User may hold several concurrently — web, desktop,
and mobile are three Sessions, not one.
_Avoid_: token, connection, login (the login is the act; the Session is the state it leaves behind)

**Book**:
The complete set of one User's accounts, transactions, and labels — the whole of what that User
records. It is the unit of ownership and of sharing: you own your Book, and you share your Book.
Exactly one Book per User for now. A Book is what a Share Link or a View Grant points at.
_Avoid_: ledger (the ledger is the double-entry core; a Book is one User's slice of it),
workspace, tenant

**Owner**:
The relationship a User has to their own Book — the one and only relationship that may *write*. A
User owns exactly one Book (their own) and may be a viewer of many.
_Avoid_: admin, author

**Viewer**:
A relationship granting *read-only* access to a Book the User does not own. Two flavours, by how
access is proved: an **authenticated viewer** is a User holding a View Grant on a Book; an
**anonymous viewer** is whoever holds a valid Share Link, with no identity at all. Same bytes come
back; different proof of entitlement.
_Avoid_: guest, reader, collaborator (a viewer never writes)

**View Grant**:
The record that a named User may read another User's Book — the authenticated-viewer counterpart
to a Share Link. The owner extends it; the viewer signs in as themselves to use it.
_Avoid_: permission, ACL entry, membership

**Share Link**:
A bearer capability — an unguessable token carried in a URL — granting anonymous, read-only access
to a whole Book. It names no User; holding it is the entire claim. This is the "anyone with the
link" flavour of sharing.
_Avoid_: invite, public URL, share token
