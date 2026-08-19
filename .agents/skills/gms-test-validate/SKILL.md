---
name: gms-test-validate
description: Validate targeted GAS Management System Java, JavaScript, template, or UI changes with the smallest relevant checks before declaring the task ready.
---

# GMS targeted validation

Use this skill after implementation or when the user asks to validate changes.

1. Identify the modified surface and its closest controller/service/JS tests.
2. Run the most specific tests first, then expand only when risk or dependencies justify it.
3. For changed JavaScript, run `node --check <archivo>`.
4. Run `git diff --check`.
5. For UI changes, state the manual browser validation required.
6. Run the complete suite only when scope and risk justify it.

Prefer affected controller/service tests. Do not routinely run MySQL/concurrency integrations. Use the Maven configuration already established and available in the environment. `./mvnw` is only a portable example; if the environment uses external Maven, reuse that installation. Do not install another Maven, modify `pom.xml` or project configuration just to run tests, or embed personal absolute paths.

Read [references/validation-matrix.md](references/validation-matrix.md) for choosing checks. Report commands, results, skipped checks and manual validation.
