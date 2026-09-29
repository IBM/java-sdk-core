# AGENTS.md

This file provides guidance to agents when working with code in this repository.

## Non-Obvious Coding Rules

- **Never instantiate `Gson` directly.** Always use `GsonSingleton.getGson()` or `getGsonWithoutPrettyPrinting()`. It has custom type adapters for `Date`, `byte[]`, `LazilyParsedNumber`, `DynamicModel`, and discriminator-based polymorphism already registered — direct `Gson` instances will miss these.
- **Checkstyle runs during `mvn test`** and will fail the build. Run `mvn checkstyle:check` early to catch style issues before tests.
- **No wildcard imports** (`import foo.*`) — Checkstyle rejects them. All imports must be explicit.
- **Unused imports fail the build**, including imports used only in Javadoc `{@link ...}` tags (controlled by `processJavadoc=true`).
- **Utility classes must have a private no-arg constructor** — `HideUtilityClassConstructor` check is active. Add `private MyUtil() { }` to any class that has only static methods.
- Test fixtures in `src/test/resources/` are loaded by passing the full relative path string: `TestUtils.loadFixture("src/test/resources/iam_token.json", IamToken.class)`.
- **`Clock` utility** must be used for token expiry logic (not `System.currentTimeMillis()` directly) so tests can mock time without sleeping.
- Authenticator builders use the pattern `new XxxAuthenticator.Builder().<fields>.build()` — do not use constructors directly as they are deprecated.
- When writing new authenticator validation error messages, use the existing constants on `AuthenticatorBase` (e.g. `ERRORMSG_PROP_MISSING`, `ERRORMSG_PROP_INVALID`) with `String.format()`.
