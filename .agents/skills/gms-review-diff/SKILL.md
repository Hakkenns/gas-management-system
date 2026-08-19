---
name: gms-review-diff
description: Read-only review of GAS Management System changes for scope, accidental files, debug residue, formatting, secrets and readiness before closeout.
---

# GMS diff review

This skill is read-only by design. Use it for “revisa el diff”, “audita los cambios” or “comprueba si está listo”. Never stage, commit or push.

Run:

```text
git status --short
git diff --stat
git diff
git diff --check
git ls-files --others --exclude-standard
```

Review unexpected and untracked files, scope creep, temporary debug, formatting churn, accidental changes, secrets/local data, unnecessary commented code, suspicious tests and unrelated files. Report `APTO` or `NO APTO` with concrete findings and affected paths.
