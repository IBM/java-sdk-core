[![Build Status](https://github.com/IBM/java-sdk-core/actions/workflows/build.yaml/badge.svg)](https://github.com/IBM/java-sdk-core/actions/workflows/build.yaml)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.ibm.cloud/sdk-core/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.ibm.cloud/sdk-core)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![semantic-release](https://img.shields.io/badge/%20%20%F0%9F%93%A6%F0%9F%9A%80-semantic--release-e10079.svg)](https://github.com/semantic-release/semantic-release)
[![CLA assistant](https://cla-assistant.io/readme/badge/ibm/java-sdk-core)](https://cla-assistant.io/ibm/java-sdk-core)

# IBM Java SDK Core Version 9.23.0
This project contains core functionality required by Java code generated by the IBM Cloud OpenAPI SDK Generator
(openapi-sdkgen).

## Installation
The current version of the project: 9.23.0

You can use this project by defining it as a dependency within your Java SDK project
(i.e. a project containing Java code generated by the IBM Cloud OpenAPI SDK Generator).

##### Maven
```xml
<dependency>
	<groupId>com.ibm.cloud</groupId>
	<artifactId>sdk-core</artifactId>
	<version>9.23.0</version>
</dependency>
```

##### Gradle

```gradle
'com.ibm.cloud:sdk-core:9.23.0'
```

## Prerequisites
- Java SE version 8 or newer is required

## Javadoc
You can find the Javadoc for this project here: https://ibm.github.io/java-sdk-core/

## Authentication
The java-sdk-core project supports the following types of authentication:
- Basic Authentication
- Bearer Token Authentication
- Identity and Access Management (IAM) Authentication (grant type: apikey)
- Identity and Access Management (IAM) Authentication (grant type: assume)
- Container Authentication
- VPC Instance Authentication
- Cloud Pak for Data Authentication
- Multi-Cloud Saas Platform (MCSP) Authentication
- No Authentication (for testing)

For more information about the various authentication types and how to use them with your services, click [here](Authentication.md).

## Logging
This project uses the [java.util.logging](https://docs.oracle.com/en/java/javase/11/core/java-logging-overview.html)
framework for logging errors, warnings, informational and debug messages. The output is controlled by configuring the desired
logging level (SEVERE, WARNING, INFO, FINE, etc.) for various loggers.

Each class within the project creates its own logger which is named after the class (e.g. `com.ibm.cloud.sdk.core.service.BaseService`).
The logger names form a hierarchy that mirrors the package/class hierarchy. A logging level can be configured for an individual logger
or for a group of loggers by leveraging the package name hierarchy.

You can configure the logging framework programmatically by using the [java.util.logging API](https://docs.oracle.com/javase/8/docs/api/java/util/logging/package-summary.html) to create and configure
loggers, handlers and formatters. Please consult the logging framework documentation for more details on this.

You can also configure the framework by defining the desired configuration in a file, then
supplying the name of the file to the logging framework when you run your application.
Included with this project are two files which serve as examples of how to configure the logging framework:
* `logging.properties`: this file contains the default configuration which will display messages that are logged at
level INFO and above (e.g. INFO, WARNING, SEVERE) on the console
* `debug-logging.properties`: this file contains a sample configuration that will set the logging level to FINE for all
Java SDK core loggers and INFO for all other loggers

To cause the logging framework to use a particular configuration file when running your java application, specify the
name of the configuration file with the `java.util.logging.config.file` system property, like this:
```
    java -jar myapp.jar -Djava.util.logging.config.file=debug-logging.properties mypackage.MyMainClass
```

For more details regarding the `java.util.logging` API and configuration, please consult the appropriate
`java.util.logging` documentation.

### HTTP message logging
One particular logging-related subject worth mentioning is the logging of HTTP request and response
messages as they flow back and forth between the client and server.
This can be very useful in diagnosing problems or simply verifying that the application is working as intended
(we all know that sometimes "working as intended" is not necessarily aligned with "working as implemented" :) ).

The easiest way to enable HTTP message logging in the Java SDK core library is to simply configure
logging level FINE (or FINER or FINEST) for the `com.ibm.cloud.sdk.core.service.BaseService` logger.
This has the side-effect of causing an interceptor to be registered with the underlying `okhttp3` HTTP transport layer which performs
logging of HTTP request and response messages. Note that the `debug-logging.properties` file contains a configuration that will
include HTTP message logging.


## Issues

If you encounter an issue with this project, you are welcome to submit a [bug report](https://github.com/IBM/java-sdk-core/issues).
Before opening a new issue, please search for similar issues. It's possible that someone has already reported it.

## Open source @ IBM

Find more open source projects on the [IBM Github Page](http://github.com/IBM)

## License

This library is licensed under Apache 2.0. Full license text is
available in [LICENSE](LICENSE).

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md).

## Code of conduct

See [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md).
