# AGENTS.md

This file provides guidance to agents when working with code in this repository.

## Project

Java Maven library (`com.ibm.cloud:sdk-core`) — the core runtime for IBM Cloud Java SDKs. Compiles to Java 8 bytecode; requires JDK 11+ to build.

## Commands

```bash
# Build + test + checkstyle (all-in-one)
mvn clean package

# Skip tests (build only)
mvn clean package -DskipTests

# Run all tests (with checkstyle)
mvn test

# Run a single test class
mvn test -Dtest=IamAuthenticatorTest

# Run a single test method
mvn test -Dtest=IamAuthenticatorTest#testSomeMethod

# Lint only (checkstyle)
mvn checkstyle:check
```

Checkstyle runs as part of the `test` phase — it will fail the build. Config is at [`build/checkstyle.xml`](build/checkstyle.xml); suppressions at [`build/checkstyle-suppressions.xml`](build/checkstyle-suppressions.xml).

## Code Style (enforced by Checkstyle)

- **No tabs** — spaces only
- **Max line length: 120 characters**
- **Max method length: 200 lines**
- **No wildcard imports** (`import com.foo.*` is rejected)
- **No unused imports** (including those only referenced in Javadoc)
- **Newline at end of file**
- **No trailing spaces**
- `if (` and `for (` require a space before the parenthesis
- Modifier order enforced; redundant modifiers rejected
- Utility classes (no instance state) must have a private constructor — checked by `HideUtilityClassConstructor`
- Array style: `String[] args` not `String args[]`

## Test Framework & Patterns

- **TestNG** (not JUnit for test logic) — use `org.testng.annotations.Test`, `@BeforeMethod`, `@AfterMethod`
- Assert imports: `static org.testng.Assert.*`
- Unit tests that need an HTTP server extend [`BaseServiceUnitTest`](src/test/java/com/ibm/cloud/sdk/core/test/BaseServiceUnitTest.java), which starts/stops an OkHttp `MockWebServer`
- JSON fixtures are loaded via `TestUtils.loadFixture(filename, Class)` from [`src/test/resources/`](src/test/resources/) — filenames passed as literal paths (e.g. `"src/test/resources/iam_token.json"`)
- Live/integration tests are annotated `@Ignore` and are never run in CI; they require a hand-crafted `.env` file at the project root

## Key Singletons & Utilities

- **`GsonSingleton`** — always use this instead of `new Gson()`. Use `getGson()` (pretty-printed) or `getGsonWithoutPrettyPrinting()` for serialization. Custom type adapters for `Date`, `byte[]`, `LazilyParsedNumber`, `DynamicModel`, and discriminator-based types are already registered.
- **`CredentialUtils.getServiceProperties(serviceName)`** — reads config from credential file → env vars → VCAP_SERVICES → system properties (in that priority order). Service name is uppercased and `-` → `_` for env/file lookup but not for VCAP.
- **`Clock`** utility exists for time-based token expiry logic — mock it in tests instead of sleeping.

## Architecture

- `BaseService` (abstract) → extended by generated SDK service classes. Uses `OkHttpClient` (via `HttpClientSingleton`) for HTTP.
- `Authenticator` interface → `AuthenticatorBase` → concrete implementations (IAM, CP4D, MCSP, etc.)
- Token-based authenticators extend `TokenRequestBasedAuthenticator`; they auto-refresh tokens before expiry.
- Builder pattern used throughout authenticators (e.g. `new IamAuthenticator.Builder().apikey(...).build()`).
- All error messages for validation are constants on `AuthenticatorBase` (e.g. `ERRORMSG_PROP_MISSING`).

## Commit Convention

Angular-style commit messages required — `semantic-release` uses them to determine version bumps and changelogs (e.g. `fix(IAM): ...` → patch, `feat(BaseService): ...` → minor, `BREAKING CHANGE:` footer → major).
