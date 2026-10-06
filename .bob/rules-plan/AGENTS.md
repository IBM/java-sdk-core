# AGENTS.md

This file provides guidance to agents when working with code in this repository.

## Architecture Constraints (Non-Obvious)

- **Token-based authenticators are stateful** — they cache the current access token and auto-refresh before expiry. The `TokenRequestBasedAuthenticator` base class owns this logic; subclasses only implement token fetching. Do not add token caching elsewhere.
- **`GsonSingleton` is the central Gson registry** — all type adapters (polymorphism via discriminator, `DynamicModel`, `byte[]` base64, `Date` as datetime) are registered there. Any new type adapter must be added in `GsonSingleton.registerTypeAdapters()`, not in individual classes.
- **`HttpClientSingleton`** manages the single shared `OkHttpClient`. Per-service or per-authenticator client customization goes through `HttpConfigOptions` passed to `configureClient()`, not by creating new clients.
- **Credential resolution is layered and order-sensitive**: credential file → environment variables → VCAP_SERVICES → system properties. The service name is transformed (uppercase, `-`→`_`) for file/env lookups but not for VCAP. Changing this order is a breaking change.
- **`DiscriminatorBasedTypeAdapterFactory`** enables polymorphic deserialization via a JSON field discriminator — this is the mechanism generated SDK models use for oneOf/anyOf schemas. Do not use plain Gson polymorphism patterns.
- **Source/target is Java 8** (`pom.xml` `java-source-version`/`java-target-version` = 1.8) even though JDK 11+ is required to build. No Java 9+ APIs in main source.
- **`reuseForks=false`** in Surefire — each test class runs in a fresh JVM fork. Tests that mutate system properties or static singletons will not leak between classes, but this makes the test run slow.
