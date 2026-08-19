---
name: gms-ui-cleanup
description: Clean up an existing GAS Management System AdminLTE/Bootstrap view, modal, card, table, form, spacing, hierarchy, or responsive layout without changing its application logic.
---

# GMS UI cleanup

Use this skill for focused visual improvements in the Thymeleaf/AdminLTE UI.

- Inspect the target view and a nearby approved pattern before editing.
- Reuse existing AdminLTE, Bootstrap and Font Awesome classes.
- Make the smallest focused visual change; preserve endpoints, selectors and behavior.
- Check responsive behavior, modal/backdrop stacking, table overflow, spacing and hierarchy.
- Avoid new frameworks, aggressive global CSS, and global overrides of `.btn`, `.container` or `.form-control`.
- Do not copy inherited inline styles as a new pattern.

## Stop conditions

Stop and report before expanding scope if the requested visual cleanup requires:

- modifying endpoints or backend contracts;
- changing JavaScript contracts;
- altering business rules;
- functional refactoring;
- a global redesign of the system;
- modifying components outside the target view's scope;
- introducing a new visual framework.

The skill may identify the issue, but must not expand the task automatically.

Read [references/visual-checklist.md](references/visual-checklist.md) when doing the visual review. Report changed files, visual checks and any manual browser verification still needed.
