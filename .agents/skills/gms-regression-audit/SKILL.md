---
name: gms-regression-audit
description: Perform a read-only, adversarial regression audit of a GAS Management System module across behavior, business logic, feedback, permissions, lifecycle, persistence and UI without automatically fixing findings.
---

# GMS regression audit

Use this skill before closing a substantial functional or UI module when the goal is to find how it can fail, not merely demonstrate that it works.

The default contract is **AUDITAR != CORREGIR**. Inspect the real module contract first, then challenge it with happy paths, negative paths, boundary values, invalid states, repeated navigation, inconsistent data, backend/frontend failures, persistence, roles, lifecycle and UX. Do not infer business rules that are not present in the repository.

- Apply only actions that the module actually exposes.
- Distinguish backend defects from correct backend behavior with incorrect UX.
- Classify each finding by type and severity; do not present hypotheses as facts.
- Use existing tests and tooling when appropriate, but do not install dependencies, change project configuration or run heavy MySQL/concurrency checks routinely.
- When AppModules is present, inspect lifecycle, stale async work, listeners, timers, DataTables, modals and navigation scenarios.
- When the module has wide UI, include sidebar expanded → collapsed → expanded and responsive checks.
- Audit permission consistency without restructuring global security.
- Do not fix findings, modify application code or broaden scope automatically.

Read [references/regression-matrix.md](references/regression-matrix.md) for the reusable audit matrix, classification scheme and finding format. Integrate conceptually with `gms-feedback-ui`, `gms-appmodules-navigation`, `gms-ui-cleanup`, `gms-test-validate`, `gms-review-diff` and `gms-safe-closeout`; do not duplicate their full workflows.

## Stop condition

Finish with a report. A later task must explicitly authorize which findings to correct, the files in scope and the validation required.
