# Authentication
The java-sdk-core project supports the following types of authentication:
- Basic Authentication
- Bearer Token 
- Identity and Access Management (IAM)
- Cloud Pak for Data
- No Authentication

The SDK user configures the appropriate type of authentication for use with service instances.  
The authentication types that are appropriate for a particular service may vary from service to service, so it is important for the SDK user to consult with the appropriate service documentation to understand which authenticators are supported for that service.

The java-sdk-core allows an authenticator to be specified in one of two ways:
1. programmatically - the SDK user invokes the appropriate constructor to create an instance of the desired authenticator and then passes the authenticator instance when constructing an instance of the service.
2. configuration - the SDK user provides external configuration information (in the form of environment variables or a credentials file) to indicate the type of authenticator along with the configuration of the necessary properties for that authenticator.  The SDK user then invokes the configuration-based authenticator factory to construct an instance of the authenticator that is described in the external configuration information.

The sections below will provide detailed information for each authenticator
which will include the following:
- A description of the authenticator
- The properties associated with the authenticator
- An example of how to construct the authenticator programmatically
- An example of how to configure the authenticator through the use of external
configuration information.  The configuration examples below will use
environment variables, although the same properties could be specified in a
credentials file instead.

## Basic Authentication
The `BasicAuthenticator` is used to add Basic Authentication information to
each outbound request in the `Authorization` header in the form:
```
   Authorization: Basic <encoded username and password>
```
### Properties
- username: (required) the basic auth username
- password: (required) the basic auth password
### Programming example
```java
import com.ibm.cloud.sdk.core.security.BasicAuthenticator;
import <sdk_base_package>.ExampleService.v1.ExampleService;
...
// Create the authenticator.
BasicAuthenticator authenticator = new BasicAuthenticator("myuser", "mypassword");

// Create the service instance.
ExampleService service = new ExampleService(authenticator);

// 'service' can now be used to invoke operations.
```
### Configuration example
External configuration:
```
export EXAMPLE_SERVICE_AUTH_TYPE=basic
export EXAMPLE_SERVICE_USERNAME=myuser
export EXAMPLE_SERVICE_PASSWORD=mypassword
```
Application code:
```java
import com.ibm.cloud.sdk.core.security.ConfigBaseAuthenticatorFactory;
import <sdk_base_package>.ExampleService.v1.ExampleService;
...
// Create the authenticator.
Authenticator authenticator = ConfigBasedAuthenticatorFactory.getAuthenticator("example_service");

// Create the service instance.
ExampleService service = new ExampleService(authenticator);

// 'service' can now be used to invoke operations.
```

## Bearer Token Authentication
The `BearerTokenAuthenticator` will add a user-supplied bearer token to
each outbound request in the `Authorization` header in the form:
```
    Authorization: Bearer <bearer-token>
```
### Properties
- bearerToken: (required) the bearer token value
### Programming example
```java
import com.ibm.cloud.sdk.core.security.BearerTokenAuthenticator;
import <sdk_base_package>.ExampleService.v1.ExampleService;
...
String bearerToken = // ... obtain bearer token value ...

// Create the authenticator.
BearerTokenAuthenticator authenticator = new BearerTokenAuthenticator(bearerToken);

// Create the service instance.
ExampleService service = new ExampleService(authenticator);

// 'service' can now be used to invoke operations.
...
// Later, if your bearer token value expires, you can set a new one like this:
newToken = // ... obtain new bearer token value
authenticator.setBearerToken(newToken);
```
### Configuration example
External configuration:
```
export EXAMPLE_SERVICE_AUTH_TYPE=bearertoken
export EXAMPLE_SERVICE_BEARER_TOKEN=<the bearer token value>
```
Application code:
```java
import com.ibm.cloud.sdk.core.security.ConfigBaseAuthenticatorFactory;
import com.ibm.cloud.sdk.core.security.BearerTokenAuthenticator;
import <sdk_base_package>.ExampleService.v1.ExampleService;
...
// Create the authenticator.
Authenticator authenticator = ConfigBasedAuthenticatorFactory.getAuthenticator("example_service");

// Create the service instance.
ExampleService service = new ExampleService(authenticator);

// 'service' can now be used to invoke operations.
...
// Later, if your bearer token value expires, you can set a new one like this:
newToken = // ... obtain new bearer token value
((BearerTokenAuthenticator) authenticator).setBearerToken(newToken);
```
Note that the use of external configuration is not as useful with the `BearerTokenAuthenticator` as it
is for other authenticator types because bearer tokens typically need to be obtained and refreshed
programmatically since they normally have a relatively short lifespan before they expire.  This
authenticator type is intended for situations in which the application will be managing the bearer 
token itself in terms of initial acquisition and refreshing as needed.

## Identity and Access Management Authentication (IAM)
The `IamAuthenticator` will accept a user-supplied api key and will perform
the necessary interactions with the IAM token service to obtain a suitable
bearer token for the specified api key.  The authenticator will also obtain 
a new bearer token when the current token expires.  The bearer token is 
then added to each outbound request in the `Authorization` header in the
form:
```
   Authorization: Bearer <bearer-token>
```
### Properties
- apikey: (required) the IAM api key
- url: (optional) The URL representing the IAM token service endpoint.  If not specified, a suitable
default value is used.
- clientId/clientSecret: (optional) The `clientId` and `clientSecret` fields are used to form a 
"basic auth" Authorization header for interactions with the IAM token server. If neither field 
is specified, then no Authorization header will be sent with token server requests.  These fields 
are optional, but must be specified together.
- disableSSLVerification: (optional) A flag that indicates whether verificaton of the server's SSL 
certificate should be disabled or not. The default value is `false`.
- headers: (optional) A set of key/value pairs that will be sent as HTTP headers in requests
made to the IAM token service.
### Programming example
```java
import com.ibm.cloud.sdk.core.security.IamAuthenticator;
import <sdk_base_package>.ExampleService.v1.ExampleService;
...
// Create the authenticator.
IamAuthenticator authenticator = new IamAuthenticator("myapikey");

// Create the service instance.
ExampleService service = new ExampleService(authenticator);

// 'service' can now be used to invoke operations.
```
### Configuration example
External configuration:
```
export EXAMPLE_SERVICE_AUTH_TYPE=iam
export EXAMPLE_SERVICE_APIKEY=myapikey
```
Application code:
```java
import com.ibm.cloud.sdk.core.security.ConfigBaseAuthenticatorFactory;
import <sdk_base_package>.ExampleService.v1.ExampleService;
...
// Create the authenticator.
Authenticator authenticator = ConfigBasedAuthenticatorFactory.getAuthenticator("example_service");

// Create the service instance.
ExampleService service = new ExampleService(authenticator);

// 'service' can now be used to invoke operations.
```

##  Cloud Pak for Data
The `CloudPakForDataAuthenticator` will accept user-supplied username and password values, and will 
perform the necessary interactions with the Cloud Pak for Data token service to obtain a suitable
bearer token.  The authenticator will also obtain a new bearer token when the current token expires.
The bearer token is then added to each outbound request in the `Authorization` header in the
form:
```
   Authorization: Bearer <bearer-token>
```
### Properties
- username: (required) the username used to obtain a bearer token.
- password: (required) the password used to obtain a bearer token.
- url: (required) The URL representing the Cloud Pak for Data token service endpoint.
- disableSSLVerification: (optional) A flag that indicates whether verificaton of the server's SSL 
certificate should be disabled or not. The default value is `false`.
- headers: (optional) A set of key/value pairs that will be sent as HTTP headers in requests
made to the IAM token service.
### Programming example
```java
import com.ibm.cloud.sdk.core.security.CloudPakForDataAuthenticator;
import <sdk_base_package>.ExampleService.v1.ExampleService;
...
// Create the authenticator.
CloudPakForDataAuthenticator authenticator = new CloudPakForDataAuthenticator("https://mycp4dhost.com/", "myuser", "mypassword");

// Create the service instance.
ExampleService service = new ExampleService(authenticator);

// 'service' can now be used to invoke operations.
```
### Configuration example
External configuration:
```
export EXAMPLE_SERVICE_AUTH_TYPE=cp4d
export EXAMPLE_SERVICE_USERNAME=myuser
export EXAMPLE_SERVICE_PASSWORD=mypassword
export EXAMPLE_SERVICE_URL=https://mycp4dhost.com/
```
Application code:
```java
import com.ibm.cloud.sdk.core.security.ConfigBaseAuthenticatorFactory;
import <sdk_base_package>.ExampleService.v1.ExampleService;
...
// Create the authenticator.
Authenticator authenticator = ConfigBasedAuthenticatorFactory.getAuthenticator("example_service");

// Create the service instance.
ExampleService service = new ExampleService(authenticator);

// 'service' can now be used to invoke operations.
```

## No Auth Authentication
The `NoAuthAuthenticator` is a placeholder authenticator which performs no actual authentication function.   It can be used in situations where authentication needs to be bypassed, perhaps while developing or debugging an application or service.
### Properties
None
### Programming example
```java
import com.ibm.cloud.sdk.core.security.NoAuthAuthenticator;
import <sdk_base_package>.ExampleService.v1.ExampleService;
...
// Create the authenticator.
NoAuthAuthenticator authenticator = new NoAuthAuthenticator();

// Create the service instance.
ExampleService service = new ExampleService(authenticator);

// 'service' can now be used to invoke operations.
```
### Configuration example
External configuration:
```
export EXAMPLE_SERVICE_AUTH_TYPE=noauth
```
Application code:
```java
import com.ibm.cloud.sdk.core.security.ConfigBaseAuthenticatorFactory;
import <sdk_base_package>.ExampleService.v1.ExampleService;
...
// Create the authenticator.
Authenticator authenticator = ConfigBasedAuthenticatorFactory.getAuthenticator("example_service");

// Create the service instance.
ExampleService service = new ExampleService(authenticator);

// 'service' can now be used to invoke operations.
```
