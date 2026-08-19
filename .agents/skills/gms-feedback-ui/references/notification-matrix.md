# Notification matrix

| Situation | Initial behavior |
|---|---|
| Dangerous action | Large SweetAlert with explicit confirm/cancel buttons. |
| Cancel | Stop immediately; no backend, reload or notification. |
| Frequent operation succeeds | Refresh relevant UI, then animated `top-end` success toast. |
| Important error | Large SweetAlert with actionable error text. |
| Important creation/registration | Large success dialog may be retained when useful. |
| Validation warning | Use the least disruptive suitable feedback; do not standardize every validation as a modal. |

Initial standard toast:

```js
Swal.fire({
  toast: true,
  position: 'top-end',
  icon: 'success',
  showConfirmButton: false,
  timer: 3000,
  timerProgressBar: true,
  animation: true
});
```

Pause/resume the timer on mouse enter/leave when the toast is interactive or long enough to require it.
