---
name: gms-appmodules-navigation
description: Migrate or repair a GAS Management System module using its partial-navigation AppModules, init/destroy, lifecycle guards, DataTables, modal cleanup and History API contract.
---

# GMS partial navigation

Use this skill for module migration, lifecycle bugs, duplicate listeners, stale async updates, DataTables corruption or partial-navigation behavior.

- Expose `window.AppModules.<modulo>` with `init()` and `destroy()`; do not add generic globals.
- Scope DOM work to the module root and make `init()` idempotent.
- Track a `lifecycleId` or equivalent, guard async callbacks, and use `root.isConnected` when appropriate.
- Register every native/jQuery listener for cleanup; namespace jQuery events.
- Abort owned requests and timers in `destroy()`.
- Destroy DataTables before replacing their table and re-initialize after replacement.
- Clean module modals and orphaned backdrops.
- Respect the existing one-time script loading, fragment endpoints, `pushState`, `popstate`, sidebar active state and direct navigation.
- Validate A→B, B→A, A→B→A, back, forward, direct navigation, modals and tables.

Read [references/navigation-contract.md](references/navigation-contract.md) before changing a module. The known concurrent-navigation race in `navegacion.js` is a documented risk; do not modify that file automatically during an unrelated module migration.
