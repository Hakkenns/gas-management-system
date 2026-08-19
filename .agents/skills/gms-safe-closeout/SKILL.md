---
name: gms-safe-closeout
description: Prepare a validated GAS Management System task for safe closeout, separating READY, STAGED, COMMITTED and PUSHED without implicit Git publication.
---

# GMS safe closeout

Use only after implementation and relevant validation are complete, including manual UI validation when applicable. Start from a READY CANDIDATE: review repository state and diff, run or confirm `gms-review-diff`, check validation evidence, and require human approval of the diff before any staging.

- If unrelated local changes are mixed with the task, STOP.
- READY CANDIDATE → state/diff review → `gms-review-diff` evidence → validation evidence → human diff approval → exact staging only if authorized.
- Stage only exact authorized paths; never use `git add .`, `git add -A` or `git add --all`.
- Review the staged diff after authorized staging.
- Commit only with explicit authorization.
- Push only with explicit authorization.
- “Close the task” does not mean commit or push.

Use the states `READY`, `STAGED`, `COMMITTED` and `PUSHED` precisely. Report the final state, exact paths included/excluded, approvals and any blocker.
