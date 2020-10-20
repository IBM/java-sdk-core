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
