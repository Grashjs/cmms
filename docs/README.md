# docs/

Design documentation for work that spans more than one commit.

This folder and `CLAUDE.md` at the repository root have different jobs, and keeping them
apart is the point:

- **`CLAUDE.md`** — operational knowledge for changing this codebase. Build commands, deploy
  traps, conventions, the upstream divergence list. Read before touching things; short by
  design, because everything in it competes for attention.
- **`docs/`** — the reasoning behind a subsystem. What problem it solves, what was
  considered and rejected, what the contract is, and what the next step would be. Long-form,
  one file per subsystem, written so that someone picking the work up in six months does not
  have to reconstruct the decisions from the diff.

A pointer from `CLAUDE.md` to the relevant file here belongs in the same commit that adds
one. Documentation nobody is routed to is documentation nobody reads.

## Contents

| File | Subsystem |
|---|---|
| [reporting.md](reporting.md) | Reporting and BI: the `rpt_*` views, saved list views, filtered exports, and the roadmap beyond them |
| [terminology-de.md](terminology-de.md) | German wording: which terms were changed, and the 155 keys still to migrate |

## What belongs in a file here

Something a future change would get wrong without it. In practice that is:

- the reason a design is the way it is, especially where the obvious alternative was rejected
- contracts other things depend on (a view's columns, an API's stable keys)
- deliberate deviations from correctness — a mirrored rounding bug, an inherited N+1 — with
  the reason they were kept, so nobody "fixes" one in isolation
- what the next stage looks like, and what would justify starting it

What does not belong here: anything the code already says clearly, and anything that will be
stale within a month (in-progress task lists, current data volumes).
