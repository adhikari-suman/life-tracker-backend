# Labels form a three-deep tree, any node taggable, rolled up at read time

Labels are a parent-child tree, **at most three levels deep**, with no cycles and sibling names unique
case-insensitively (so `other` may live under both `food` and `transport`, and the two are told apart
by their path). **Any node may be tagged directly** — root, intermediate or leaf — so you can record
`food` when you don't yet know it was `fast food`.

A posting stores **only the node chosen**. The ancestors it rolls up into are **derived at read time**
by walking parent pointers, never copied onto the posting. A label's total in a summary is therefore
its own directly-tagged postings *plus* everything beneath it.

**Deletion is allowed only for a label that is unused and childless.** Anything else is **archived** —
hidden from the picker while history still reads it — or, later, **merged** into another label.

## Considered options

- **Freezing each posting's ancestor chain at tag time.** Rejected: you could then never fix a
  taxonomy mistake you had been living with, because postings tagged before a reorganization would
  roll up one way and later ones another — a split-brain tree with no single truth. Deriving at read
  time makes reparenting retroactive instead, and the blast radius is confined to the label
  breakdown: no balance, account total, net worth or income figure consults labels.
- **Leaf-only tagging.** Rejected: it forces a `food/other` leaf to exist purely so you can record
  *not knowing*, which is clutter standing in for an honest absence of detail.
- **Hierarchical strings (`food/fast food`) instead of entities.** Rejected: a rename would have to
  rewrite every posting that ever used the old path, and "no cycles" and a depth limit have nothing
  to check against when the tree is only implied by punctuation.
- **Delete silently reassigns to the parent.** Rejected as a *default*, though the result is not
  wrong — collapsing `fast food` into `food` preserves every ancestor total exactly, since it already
  rolled up there. But destroying years of specificity should be a thing you chose, not a side effect
  of pressing delete. That is what merge is for.
- **Delete dumps postings into Uncategorized.** Rejected outright: it yanks that spend out of its
  branch, so ancestor totals *change* and past months sprout a phantom Uncategorized spike. It is the
  one option that quietly rewrites what your reports said happened.
- **No depth cap.** Rejected: "only a few levels deep" left unenforced is a comment, not a rule, and
  the tree degenerates into a filesystem. Three covers real personal-finance taxonomies.

## Consequences

- The depth cap must be checked on **reparent** as well as create, and against the **whole subtree** —
  moving a two-deep subtree under a level-two node breaks the rule even though the moved node itself
  is fine. Checking only the node being moved is the easy bug here.
- The cap was set deliberately tight: **raising it later is safe** (existing data stays valid) while
  **lowering it breaks data already in the tree**, so three-and-raise is the recoverable direction.
- Reparenting changes past summaries. This is intended, but it means a breakdown you looked at last
  month can legitimately differ after a reorganization. Root-level totals never move; only a
  subtree's contribution shifts between branches.
- **Merge is deferred.** It is bulk-retag-then-delete, built entirely from capabilities that already
  exist, so it can be added whenever the tedium justifies it without forcing a re-cut of anything.
- Archiving is what keeps deletion honest: an obsolete label leaves the picker without erasing what it
  meant, so a purchase from two years ago never forgets what it was.
