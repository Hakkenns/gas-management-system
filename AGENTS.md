# Reglas siempre activas

- Preserva los cambios locales existentes y nunca reviertas cambios ajenos.
- Mantén el scope estrictamente limitado a la tarea solicitada.
- No introduzcas globals genéricos. En navegación parcial respeta `window.AppModules`, `init()` y `destroy()` cuando aplique.
- Toda operación async de un módulo debe comprobar que su lifecycle sigue activo antes de actualizar la UI.
- No copies como estándar heredado `alert()`, `confirm()`, listeners sin cleanup ni estilos inline.
- Ejecuta las pruebas y validaciones relevantes antes de declarar la tarea lista.
- Antes del cierre revisa `git status --short`, el diff y `git diff --check`.
- Nunca uses `git add .`, `git add -A` ni `git add --all`; el staging, si se autoriza, usa rutas exactas.
- No hagas staging automático solo porque las pruebas pasaron.
- No hagas commit ni push sin autorización explícita.
- No uses operaciones Git destructivas para limpiar el árbol.
- Antes de cerrar cambios sustanciales, ejecuta una auditoría de regresión agresiva cuando el alcance lo justifique.
