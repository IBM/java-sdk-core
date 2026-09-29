# AGENTS.md

This file provides guidance to agents when working with code in this repository.

## Documentation Context (Non-Obvious)

- The main source package is `com.ibm.cloud.sdk.core` with four subpackages: `http`, `security`, `service`, `util` — no "web" or "api" layer, this is a pure HTTP client library.
- `src/test/java/com/ibm/cloud/sdk/core/test/` contains both unit tests and shared test infrastructure (`BaseServiceUnitTest`, `TestUtils`) — the test infrastructure classes are NOT in a separate module.
- Live tests (e.g. `IamAuthenticatorLiveTest`) live alongside unit tests but are **always `@Ignore`**. They require manual `.env` files at the project root and are never run in CI.
- `ibm-credentials.env` at the project root is a sample/template credential file used by tests; it is not a real credentials file.
- The `build/` directory contains only Maven build configuration (`checkstyle.xml`, `checkstyle-suppressions.xml`) — not compiled output (that goes to `target/`).
- `Authentication.md` documents the credential resolution priority order (credential file → env vars → VCAP → system props) — useful reference when debugging config loading.
