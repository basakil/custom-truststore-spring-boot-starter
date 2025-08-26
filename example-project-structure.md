# Example Project Structure

This document shows a complete example of how to use the `CustomTrustStoreSpringBootInitializer` in a Spring Boot project.

## Project Structure

```
my-spring-boot-app/
├── src/
│   └── main/
│       ├── java/
│       │   └── com/
│       │       └── example/
│       │           └── MyApplication.java
│       ├── resources/
│       │   ├── application.yml
│       │   └── application.properties
│       └── webapp/
├── truststore/
│   └── custom/
│       └── merge/
│           ├── internal-ca.crt
│           ├── corporate-ca.cer
│           └── development-ca.pem
├── pom.xml
└── README.md
```

## Main Application Class (Recommended Approach)

```java
package com.example;

import nom.aob.truststore.spring.CustomTrustStoreSpringBootInitializer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class MyApplication {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(MyApplication.class);
        app.addInitializers(new CustomTrustStoreSpringBootInitializer());
        app.run(args);
    }
}
```

## Alternative: Using SpringApplicationBuilder

```java
package com.example;

import nom.aob.truststore.spring.CustomTrustStoreSpringBootInitializer;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class MyApplication {

    public static void main(String[] args) {
        new SpringApplicationBuilder(MyApplication.class)
            .initializers(new CustomTrustStoreSpringBootInitializer())
            .run(args);
    }
}
```

## Alternative: Static Initializer Approach

```java
package com.example;

import nom.aob.truststore.spring.CustomTrustStoreInitializer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class MyApplication {

    public static void main(String[] args) {
        // Initialize custom truststore BEFORE Spring Boot starts
        CustomTrustStoreInitializer.initialize();
        
        // Now start Spring Boot
        SpringApplication.run(MyApplication.class, args);
    }
}
```

## Maven Dependencies

```xml
<dependencies>
    <!-- Spring Boot Starter -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    
    <!-- Custom TrustStore Starter -->
    <dependency>
        <groupId>nom.aob.spring</groupId>
        <artifactId>custom-truststore-spring-boot-starter</artifactId>
        <version>1.0.9</version>
    </dependency>
</dependencies>
```

## Configuration Files

### application.yml
```yaml
# Other Spring Boot configuration...
spring:
  application:
    name: my-spring-boot-app

# Custom TrustStore configuration
truststore:
  custom:
    enabled: true
    merge-dir: "truststore/custom/merge"
    fail-on-error: false
    expose-bean: false
```

### application.properties
```properties
# Other Spring Boot configuration...
spring.application.name=my-spring-boot-app

# Custom TrustStore configuration
truststore.custom.enabled=true
truststore.custom.merge-dir=truststore/custom/merge
truststore.custom.fail-on-error=false
truststore.custom.expose-bean=false
```

## Certificate Files

Place your custom CA certificates in the `truststore/custom/merge/` directory:

- **internal-ca.crt** - Internal corporate CA certificate
- **corporate-ca.cer** - Corporate CA certificate  
- **development-ca.pem** - Development environment CA certificate

## Expected Logs

When you start the application, you should see:

```
INFO  CustomTrustStoreSpringBootInitializer - Initializing CustomTrustStore via ApplicationContextInitializer...
INFO  CustomTrustStoreSpringBootInitializer - Using certificates directory from configuration: truststore/custom/merge
INFO  CustomTrustStore - Merging custom certificates from truststore/custom/merge with original trust store.
INFO  CustomTrustStore - Successfully added certificate: internal-ca.crt
INFO  CustomTrustStore - Successfully added certificate: corporate-ca.cer
INFO  CustomTrustStore - Successfully added certificate: development-ca.pem
INFO  CustomTrustStore - Custom certificates are successfully merged into the new SSLContext.
INFO  CustomTrustStoreSpringBootInitializer - Successfully initialized custom trust store and set as default SSLContext
INFO  SpringApplication - Starting SpringApplication using Java 17
...
```

## Benefits of ApplicationContextInitializer Approach

1. **Early Initialization**: Runs before ApplicationContext is created
2. **Spring Integration**: Uses Spring Boot's property binding system
3. **Configuration Support**: Full access to application properties
4. **Bean Ready**: SSL context is ready when beans are created
5. **Standard Pattern**: Follows Spring Boot best practices
6. **Property Binding**: Automatically binds properties to CustomTrustStoreProperties
7. **Environment Access**: Can access all Spring Boot environment properties

## Troubleshooting

- **No logs?** Check that the dependency is properly included
- **Directory not found?** Ensure the certificates directory exists
- **Certificates not loading?** Verify file extensions are `.crt`, `.cer`, or `.pem`
- **SSL errors?** Check that certificates are valid X.509 format
- **Properties not reading?** Ensure property names match exactly (e.g., `truststore.custom.merge-dir`)
- **Initializer not running?** Verify you're calling `app.addInitializers()` before `app.run()`
