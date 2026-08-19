# Regression audit matrix

## Functional surface

Audit only the operations the module actually provides:

- list, consult, search, filter and paginate;
- create, edit, activate, deactivate, delete and annul;
- cancel and confirm;
- open and close modals;
- repeat an operation;
- abandon an operation midway.

For each operation, compare the UI action, request, response, persisted result and final visible state.

## Boundary and invalid data

When relevant, challenge empty, null, zero, negative, duplicate, minimum, maximum, long text, special characters, nonexistent or deactivated records, missing relationships, past and boundary dates, terminal states and historical data. First identify the real contract; do not invent domain rules.

Pay special attention only when the module involves them: inventory, containers, content, ownership, location, loans, exchange, sales, purchases, cash, payments or correlatives.

## Network and backend

Check, when possible, 400, 401, 403, 404, 409, 422 when supported, 500, empty responses, unexpected JSON, timeout, aborted requests, late responses and duplicate requests.

Look for raw status codes, `ResponseStatusException`, JSON, stack traces, technical or empty messages, destructive actions without confirmation, success shown after backend failure, cancellation that still calls the backend and important silent errors. Classify whether the backend or the UX is incorrect.

## Navigation and lifecycle

For AppModules, inspect direct load/F5, A→B→A, A→B→Back, forward, duplicate init, incomplete destroy, duplicate listeners, timers, polling, `setInterval`, `setTimeout`, `requestAnimationFrame`, `AbortController`, stale callbacks, duplicate DataTables, orphaned modals/backdrops and resources surviving the module.

Use the real `AppModules`, `init()` and `destroy()` contract. Report the known global navigation race when relevant; do not fix `navegacion.js` during a module audit.

## UI and permissions

For wide content, test sidebar expanded → collapsed → expanded. Check header/body alignment, clipping, overflow, empty gaps, duplicated controls, responsive behavior, modal viewport placement, residual backdrops, button visibility and disabled-state consistency.

When applicable, check visible actions versus endpoint permissions, profile-specific behavior and data visibility. Report risks without restructuring global security.

## Persistence and concurrency

When the environment allows it, compare UI/backend action with the persisted result. Look for false visual success, partial persistence, duplicates, incomplete rollback and accidental historical changes.

Assess concurrency only when the domain justifies it, such as stock, inventory, cash, correlatives, loans or sensitive movements. Do not routinely run MySQL or heavy tests; use targeted validation decisions from `gms-test-validate`.

## Console and automation

Inspect JavaScript errors, DataTables warnings, uncaught errors, rejected promises, unexpected 4xx/5xx, duplicate calls, polling after destroy, temporary logs and silently swallowed errors. Do not classify every warning as a bug.

Detect existing JUnit, MockMvc, Selenium, Playwright, Cypress, jsdom or other usable infrastructure. Use it when suitable. If a flow cannot be automated, state the human validation required. Do not install new dependencies.

## Classification

Every relevant finding has one type:

`REGRESIÓN`, `BUG PREEXISTENTE`, `ERROR LÓGICO`, `UX / FEEDBACK`, `SEGURIDAD / PERMISOS`, `LIFECYCLE`, `UI`, `INTEGRIDAD / PERSISTENCIA`, `CONCURRENCIA`, `RIESGO` or `NO PROBLEMA`.

Use one severity:

`CRÍTICO`, `ALTO`, `MEDIO` or `BAJO`.

Use `REGRESIÓN` versus `BUG PREEXISTENTE` only when supported by diff, previous behavior, tests or existing contracts. Otherwise report uncertainty explicitly.

## Finding format

```
ID:
Severidad:
Tipo:
Módulo:
Flujo:
Precondición:
Pasos o condición de reproducción:
Esperado:
Obtenido:
Evidencia:
Impacto:
Causa probable:
Archivos relacionados:
Automatizable: Sí / No / Parcial
Recomendación:
```

If reproduction failed, say so explicitly. End with tested scope, untested scope, findings, `NO PROBLEMA` checks where useful and a clear statement that no fixes were applied.
