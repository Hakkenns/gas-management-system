# Validation matrix

| Changed surface | First checks |
|---|---|
| Controller | Affected controller test, then related service test if behavior crosses the service boundary. |
| Service/repository | Affected service test; integration only if transaction, locking or database behavior is the subject. |
| JavaScript | `node --check <archivo>` plus manual flow checks for lifecycle/UI behavior. |
| Template/CSS | `git diff --check` plus manual desktop/narrow viewport inspection. |
| Navigation/lifecycle | Targeted JavaScript syntax check plus A→B, B→A, back/forward and modal/table checks. |
| MySQL/concurrency | Explicitly justified integration run only; never routine validation. |

Use the repository wrapper, for example:

```text
./mvnw -Dtest=AffectedTest test
```

On Windows, use the corresponding wrapper command already provided by the repository.
