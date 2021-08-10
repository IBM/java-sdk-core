# [9.12.0](https://github.com/IBM/java-sdk-core/compare/9.11.1...9.12.0) (2021-08-10)


### Features

* introduce new container authenticator ([#140](https://github.com/IBM/java-sdk-core/issues/140)) ([d6b455b](https://github.com/IBM/java-sdk-core/commit/d6b455b94c8d8059fce7a843889872b9f5a854b3))

## [9.11.1](https://github.com/IBM/java-sdk-core/compare/9.11.0...9.11.1) (2021-08-02)


### Bug Fixes

* **http:** avoid gzipping zero length content ([4c86252](https://github.com/IBM/java-sdk-core/commit/4c862522f1a3f9fdab6be82c0e18935129c9c130))

# [9.11.0](https://github.com/IBM/java-sdk-core/compare/9.10.4...9.11.0) (2021-07-12)


### Features

* **BaseService:** remove final modifier from createServiceCall ([#132](https://github.com/IBM/java-sdk-core/issues/132)) ([2e35d1f](https://github.com/IBM/java-sdk-core/commit/2e35d1f5c0056fcef1e01ec7eaf8b8bc6448cc4b))

## [9.10.4](https://github.com/IBM/java-sdk-core/compare/9.10.3...9.10.4) (2021-07-08)


### Bug Fixes

* bump maven-compiler-plugin source and target version to 1.8 ([#131](https://github.com/IBM/java-sdk-core/issues/131)) ([e8e8a40](https://github.com/IBM/java-sdk-core/commit/e8e8a401940aec7c25883e0e5be08ad2626462f2))

## [9.10.3](https://github.com/IBM/java-sdk-core/compare/9.10.2...9.10.3) (2021-06-08)


### Bug Fixes

* change method name to `constructServiceUrl` ([#126](https://github.com/IBM/java-sdk-core/issues/126)) ([4a5eae9](https://github.com/IBM/java-sdk-core/commit/4a5eae97caabdae6014f28e7929960268329f9b7))

## [9.10.2](https://github.com/IBM/java-sdk-core/compare/9.10.1...9.10.2) (2021-05-27)


### Bug Fixes

* upgrade okhttp3 dependency to v4.9.1 ([#124](https://github.com/IBM/java-sdk-core/issues/124)) ([5ae3c3a](https://github.com/IBM/java-sdk-core/commit/5ae3c3a0d0ad210003076351c29e3930adbf9dbd))

## [9.10.1](https://github.com/IBM/java-sdk-core/compare/9.10.0...9.10.1) (2021-05-19)


### Bug Fixes

* **http:** enable TLSv1.3; disable TLSv1.0 TLSv1.1 ([#123](https://github.com/IBM/java-sdk-core/issues/123)) ([3b05fd5](https://github.com/IBM/java-sdk-core/commit/3b05fd5919c41ae471b4bc343d1739867d97f9bc))

# [9.10.0](https://github.com/IBM/java-sdk-core/compare/9.9.1...9.10.0) (2021-05-18)


### Features

* add `BaseService.constructServiceURL` method ([#122](https://github.com/IBM/java-sdk-core/issues/122)) ([76eda02](https://github.com/IBM/java-sdk-core/commit/76eda02db45387993d413ac9a926e106a7c556f1))

## [9.9.1](https://github.com/IBM/java-sdk-core/compare/9.9.0...9.9.1) (2021-04-01)


### Bug Fixes

* **deps:** update dependencies to avoid vulnerabilities ([775a4d9](https://github.com/IBM/java-sdk-core/commit/775a4d906994f4a0534f485e96c416f16f9b58fb))

# [9.9.0](https://github.com/IBM/java-sdk-core/compare/9.8.1...9.9.0) (2021-03-11)


### Features

* add getQueryParam utility method to support pagination ([082e62d](https://github.com/IBM/java-sdk-core/commit/082e62ddf3739e5249371a2e41c1102908d59360))

## [9.8.1](https://github.com/IBM/java-sdk-core/compare/9.8.0...9.8.1) (2021-02-10)


### Bug Fixes

* **build:** main migration ([#111](https://github.com/IBM/java-sdk-core/issues/111)) ([cfc2bd3](https://github.com/IBM/java-sdk-core/commit/cfc2bd31b885aa422ea6dfb45313747af6093211))
* **build:** main migration release ([#112](https://github.com/IBM/java-sdk-core/issues/112)) ([b164c01](https://github.com/IBM/java-sdk-core/commit/b164c0189f1d537eacb21136b648a069cf247c34))

# [9.8.0](https://github.com/IBM/java-sdk-core/compare/9.7.1...9.8.0) (2021-02-08)


### Features

* **authenticator:** add new cp4d service authenticator ([5d5197b](https://github.com/IBM/java-sdk-core/commit/5d5197b1d2c1dbe0ff7a23c292070f2e6f12be43))

## [9.7.1](https://github.com/IBM/java-sdk-core/compare/9.7.0...9.7.1) (2021-02-08)


### Bug Fixes

* **build:** publish artifacts directly to maven central repository ([ecadef4](https://github.com/IBM/java-sdk-core/commit/ecadef407c6f1f8f435c05ef4cb93fcba64afb22))

# [9.7.0](https://github.com/IBM/java-sdk-core/compare/9.6.0...9.7.0) (2021-02-02)


### Features

* **authentication:** support cp4d /v1/authorize operation ([1144d88](https://github.com/IBM/java-sdk-core/commit/1144d88ccc108287ca3434d1244b5e9b2767b1af))

# [9.6.0](https://github.com/IBM/java-sdk-core/compare/9.5.4...9.6.0) (2021-01-28)


### Features

* wrap exceptions during client-side response processing ([8702482](https://github.com/IBM/java-sdk-core/commit/8702482a290429b64c8d2103d8a050b19c8926ef))

## [9.5.4](https://github.com/IBM/java-sdk-core/compare/9.5.3...9.5.4) (2020-11-16)


### Bug Fixes

* support additional date-time format ([771d948](https://github.com/IBM/java-sdk-core/commit/771d948b9ce11033e878b9d11f32089d691b8a2c))

## [9.5.3](https://github.com/IBM/java-sdk-core/compare/9.5.2...9.5.3) (2020-11-12)


### Bug Fixes

* handle null or empty strings as date/datetime values ([47aa1ac](https://github.com/IBM/java-sdk-core/commit/47aa1acdcf4182acb926e4b276cf8dcd1e570be1))

## [9.5.2](https://github.com/IBM/java-sdk-core/compare/9.5.1...9.5.2) (2020-11-11)


### Bug Fixes

* support up to nanosecond precision in date-time values ([b5b3b1c](https://github.com/IBM/java-sdk-core/commit/b5b3b1c6e286ed28beb75b204f82101cef12545b))

## [9.5.1](https://github.com/IBM/java-sdk-core/compare/9.5.0...9.5.1) (2020-11-10)


### Bug Fixes

* improve date and date-time serialization/deserialization ([c89cb03](https://github.com/IBM/java-sdk-core/commit/c89cb032c269f8f31c6f8b85c098e52c6ce93588)), closes [arf/planning-sdk-squad#2291](https://github.com/arf/planning-sdk-squad/issues/2291)

# [9.5.0](https://github.com/IBM/java-sdk-core/compare/9.4.1...9.5.0) (2020-11-05)


### Features

* add new DateUtils utility class ([f3e8e93](https://github.com/IBM/java-sdk-core/commit/f3e8e93f089e0754abe832a0c5d2b6a135a8a3cc)), closes [arf/planning-sdk-squad#2289](https://github.com/arf/planning-sdk-squad/issues/2289)

## [9.4.1](https://github.com/IBM/java-sdk-core/compare/9.4.0...9.4.1) (2020-10-20)


### Bug Fixes

* set correct default message in ConflictException ([2625e62](https://github.com/IBM/java-sdk-core/commit/2625e62356b33dd613ce32c2e94bed80f8efcfe2))

# [9.4.0](https://github.com/IBM/java-sdk-core/compare/9.3.0...9.4.0) (2020-09-30)


### Features

* add support for gzip request body compression and upgrade okhttp version ([#94](https://github.com/IBM/java-sdk-core/issues/94)) ([ac1f7fd](https://github.com/IBM/java-sdk-core/commit/ac1f7fd5f122188f891a7b7d8c343b8f52c45916))

# [9.3.0](https://github.com/IBM/java-sdk-core/compare/9.2.1...9.3.0) (2020-09-18)


### Features

* **IAM Authenticator:** add support for optional 'scope' property ([#93](https://github.com/IBM/java-sdk-core/issues/93)) ([46d6615](https://github.com/IBM/java-sdk-core/commit/46d6615b5cb5f67f00455bd5981194e557fcfbba))

## [9.2.1](https://github.com/IBM/java-sdk-core/compare/9.2.0...9.2.1) (2020-09-14)


### Bug Fixes

* iam/cp4d token refresh logic ([#91](https://github.com/IBM/java-sdk-core/issues/91)) ([f0484a5](https://github.com/IBM/java-sdk-core/commit/f0484a59e86b2931a29b4369c296d1ecdcf5db24))

# [9.2.0](https://github.com/IBM/java-sdk-core/compare/9.1.1...9.2.0) (2020-09-11)


### Features

* add method RequestBuilder.resolveRequestUrl ([fdbd861](https://github.com/IBM/java-sdk-core/commit/fdbd8615f1f9edb94a05a8746bfb58345d80974f))

## [9.1.1](https://github.com/IBM/java-sdk-core/compare/9.1.0...9.1.1) (2020-09-02)


### Bug Fixes

* improve serialize-nulls in additional properties ([7ac15ea](https://github.com/IBM/java-sdk-core/commit/7ac15ea9104c4100adcdaa27be451a4fea9113f5))

# [9.1.0](https://github.com/IBM/java-sdk-core/compare/9.0.1...9.1.0) (2020-08-12)


### Features

* add ratelimiter to allow transparent retry of 429 ([#89](https://github.com/IBM/java-sdk-core/issues/89)) ([5732544](https://github.com/IBM/java-sdk-core/commit/5732544d404eb34c1b9ca1114b4830043da46824))

## [9.0.1](https://github.com/IBM/java-sdk-core/compare/9.0.0...9.0.1) (2020-08-07)


### Bug Fixes

* avoid NPE when constructing dynamic model ([3241b9b](https://github.com/IBM/java-sdk-core/commit/3241b9ba4cd954eb95ccf22430c9abe0af5c2ffd))

# [9.0.0](https://github.com/IBM/java-sdk-core/compare/8.4.3...9.0.0) (2020-07-31)


### Features

* upgrade okhttp3 dependency to 3.14.9 ([71f2bbe](https://github.com/IBM/java-sdk-core/commit/71f2bbee563df9e811006c59ca595127bf2fc7b5))


### Reverts

* Revert previous commit to amend commit message ([ee86f76](https://github.com/IBM/java-sdk-core/commit/ee86f76254e0324fc63b054ef7b2c8b4b52a5a8b))


### BREAKING CHANGES

* With this change, we're removing support for Java 7.

Note: this commit is a re-introduction of commit
f4c97b09cdfdcc2cd0273d6041a23e61579892ca with a new commit message

## [8.4.3](https://github.com/IBM/java-sdk-core/compare/8.4.2...8.4.3) (2020-07-29)


### Bug Fixes

* surface ServiceResponseError outside of RuntimeError ([#83](https://github.com/IBM/java-sdk-core/issues/83)) ([349ca1a](https://github.com/IBM/java-sdk-core/commit/349ca1ad5bb8b59dfb3fbccf309be90e93740663))

## [8.4.2](https://github.com/IBM/java-sdk-core/compare/8.4.1...8.4.2) (2020-07-23)


### Bug Fixes

* support serialization of models w/lists of discriminated subclasses ([467d635](https://github.com/IBM/java-sdk-core/commit/467d63572c64a57e9eb8c5a2ecddb90aea731484)), closes [arf/planning-sdk-squad#2011](https://github.com/arf/planning-sdk-squad/issues/2011)

## [8.4.1](https://github.com/IBM/java-sdk-core/compare/8.4.0...8.4.1) (2020-07-13)


### Bug Fixes

* explicitly serialize null values found in dynamic properties ([4c6598e](https://github.com/IBM/java-sdk-core/commit/4c6598e388e6b4fd56c89d7081782cab7208b6b5))

# [8.4.0](https://github.com/IBM/java-sdk-core/compare/8.3.0...8.4.0) (2020-06-14)


### Features

* RequestBuilder changes to support JSON streaming feature ([e349979](https://github.com/IBM/java-sdk-core/commit/e34997962a7da54d3554a37bce336db221a57fb4)), closes [arf/planning-sdk-squad#901](https://github.com/arf/planning-sdk-squad/issues/901)

# [8.3.0](https://github.com/IBM/java-sdk-core/compare/8.2.0...8.3.0) (2020-05-15)


### Features

* Add setter for properties of DynamicModel ([51e50af](https://github.com/IBM/java-sdk-core/commit/51e50afc088880da3d24083aed12a1da0c0335a9))

# [8.2.0](https://github.com/IBM/java-sdk-core/compare/8.1.5...8.2.0) (2020-05-11)


### Features

* support service config via system properties ([#79](https://github.com/IBM/java-sdk-core/issues/79)) ([d5e7a27](https://github.com/IBM/java-sdk-core/commit/d5e7a27cc2b9a52f630ebd95fc0e082dd0382a43))

## [8.1.5](https://github.com/IBM/java-sdk-core/compare/8.1.4...8.1.5) (2020-05-08)


### Bug Fixes

* allow '=' character in environment config values ([afadad5](https://github.com/IBM/java-sdk-core/commit/afadad55fd2ec35419669929e6e2334580cdc7d8))

## [8.1.4](https://github.com/IBM/java-sdk-core/compare/8.1.3...8.1.4) (2020-05-01)


### Bug Fixes

* trailing slash when building request urls, remove whitespace when parsing external config properties ([#77](https://github.com/IBM/java-sdk-core/issues/77)) ([57382f1](https://github.com/IBM/java-sdk-core/commit/57382f199e7c57b94d2e8c5f467921278e733743))

## [8.1.3](https://github.com/IBM/java-sdk-core/compare/8.1.2...8.1.3) (2020-03-10)


### Bug Fixes

* **docs:** update example commit messages in CONTRIBUTING to force patch release ([a871690](https://github.com/IBM/java-sdk-core/commit/a871690848dc74ae21306bb9dacb659a97c12189))
