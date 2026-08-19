---
name: gms-feedback-ui
description: Change confirmation, success, error, alert, or toast feedback in the GAS Management System using its existing SweetAlert2 layout conventions.
---

# GMS feedback UI

Use this skill for SweetAlert2 confirmations, toasts, important errors and operation feedback.

- Do not add a CDN; SweetAlert2 is already loaded by the shared layout.
- A dangerous action uses a large SweetAlert confirmation.
- Cancel means no backend call, no reload, no toast and no extra message.
- After confirmation and backend success, refresh the relevant UI and use the standard top-right toast for frequent edit/activate/deactivate/delete/anulate actions.
- An important error uses a large SweetAlert.
- An important creation or registration may retain a large success dialog when that improves user clarity; not every success becomes a toast.
- Do not replace every `alert()` or `confirm()` mechanically; understand the flow and preserve lifecycle guards.

Read [references/notification-matrix.md](references/notification-matrix.md) for the initial decision matrix. Report the action, cancel path, backend result path and error path validated.
