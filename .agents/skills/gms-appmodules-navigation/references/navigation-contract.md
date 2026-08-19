# Navigation contract

The shared layout loads `navegacion.js`. It maps a module to a full route, fragment endpoint and script, fetches the fragment, loads the script once, destroys the previous module, replaces `#contenido-principal`, initializes the new module and updates History/sidebar state.

Each partial view supplies a root such as:

```html
<div data-modulo="compras" data-page-title="Registro de Compras">
```

Each module should provide:

```js
window.AppModules = window.AppModules || {};
window.AppModules.compras = { init, destroy };
```

The real repository pattern also uses lifecycle IDs, `AbortController`, connected-root checks, listener registries, timer registries, jQuery event namespaces, and DataTables destroy/re-init. Preserve those invariants when present.

Navigation scenarios to verify:

- direct route load;
- A → B and B → A;
- A → B → A without duplicate listeners or stale requests;
- browser back and forward;
- modal close/backdrop cleanup;
- DataTable replacement and redraw.

Known risk: `navegacion.js` does not currently provide a global navigation request token or cancellation strategy for concurrent route loads. Treat out-of-order responses as a risk to report, not as permission to refactor navigation during an unrelated task.
