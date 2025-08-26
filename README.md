# Custom TrustStore Spring Boot Starter

A Spring Boot starter that extends the default JRE truststore with custom CA certificates. This allows your Spring Boot application to trust both the default Java certificates and your custom certificates without overwriting the original truststore.

## What Happens When You Include This Dependency

**Simply including this starter in your `pom.xml` will automatically:**

1. **Auto-detect and load** custom certificates from `truststore/custom/merge/` directory
2. **Merge** your custom certificates with the JRE's default `cacerts` truststore
3. **Set** the merged truststore as the default SSL context
4. **Make** your application trust both default and custom certificates automatically

**No code changes required** - it just works out of the box!

## Overview of Usage Approaches

This starter provides **three different approaches** to initialize custom truststore certificates, each with different levels of control and complexity:

1. **Auto-Configuration** (Simplest - Recommended for Most Cases) - Just include dependency, works automatically
2. **ApplicationContextInitializer** (Most Control) - Manual initialization in main method
3. **Standalone Usage** (Non-Spring Boot) - Use `CustomTrustStore.java` directly

## Quick Start

### 1. Add Dependency

```xml
<dependency>
    <groupId>nom.aob.spring</groupId>
    <artifactId>custom-truststore-spring-boot-starter</artifactId>
    <version>1.1.1</version>
</dependency>
```

### 2. Place Certificates

Put your custom CA certificates in the `truststore/custom/merge/` directory:

```
your-app/
├── truststore/
│   └── custom/
│       └── merge/
│           ├── internal-ca.crt
│           ├── corporate-ca.cer
│           └── development-ca.pem
├── src/
├── pom.xml
└── README.md
```

### 3. That's It!

Your application now automatically trusts both default Java certificates and your custom certificates. No additional code needed!

## Usage Examples

### Approach 1: Auto-Configuration (Simplest - Recommended)

**Just include the dependency and it works automatically!**

```xml
<!-- Add to your pom.xml -->
<dependency>
    <groupId>nom.aob.spring</groupId>
    <artifactId>custom-truststore-spring-boot-starter</artifactId>
    <version>1.1.1</version>
</dependency>
```

```java
@SpringBootApplication
public class MyApplication {
    public static void main(String[] args) {
        // No additional code needed!
        SpringApplication.run(MyApplication.class, args);
    }
}
```

**What happens automatically:**
1. ✅ **Auto-detects** certificates in `truststore/custom/merge/` directory
2. ✅ **Merges** with JRE's default `cacerts` truststore
3. ✅ **Sets** as default SSL context
4. ✅ **Makes** your app trust both default and custom certificates

### Approach 2: ApplicationContextInitializer (Most Control)

The `CustomTrustStoreSpringBootInitializer` provides the most control over when the custom truststore is initialized. It runs before the ApplicationContext is created, ensuring SSL context is ready for all beans.

#### Basic Usage in Main Method

```java
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

#### Alternative: Using SpringApplicationBuilder

```java
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

### Approach 3: Standalone Usage (Non-Spring Boot Projects)

**For non-Spring Boot projects, use the core `CustomTrustStore.java` directly:**

#### Basic Standalone Usage

```java
import nom.aob.truststore.CustomTrustStore;
import javax.net.ssl.SSLContext;

public class MyStandaloneApplication {
    public static void main(String[] args) {
        // Initialize custom truststore
        CustomTrustStore customTrustStore = new CustomTrustStore("truststore/custom/merge");
        
        // Get merged SSL context
        SSLContext sslContext = customTrustStore.getMergedSSLContext();
        
        // Set as default SSL context
        SSLContext.setDefault(sslContext);
        
        // Now your application trusts both default and custom certificates
        // ... rest of your application code
    }
}
```

#### Advanced Standalone Usage

```java
import nom.aob.truststore.CustomTrustStore;
import javax.net.ssl.SSLContext;

public class MyAdvancedApplication {
    public static void main(String[] args) {
        // Use custom original truststore
        CustomTrustStore customTrustStore = new CustomTrustStore(
            "truststore/custom/merge",                    // Custom certificates directory
            "/path/to/original/truststore.jks",          // Original truststore file
            "original-password"                           // Original truststore password
        );
        
        // Get merged SSL context
        SSLContext sslContext = customTrustStore.getMergedSSLContext();
        
        // Set as default SSL context
        SSLContext.setDefault(sslContext);
        
        // ... rest of your application code
    }
}
```

## Configuration Properties

You can customize the behavior using these application properties:

### `truststore.custom.enabled`
- **Default**: `true`
- **Description**: Enable/disable the custom truststore functionality. When disabled, the initializer will just return, without changing anything.
- **Example**: `truststore.custom.enabled=false`

### `truststore.custom.merge-dir`
- **Default**: `truststore/custom/merge`
- **Description**: Directory path containing custom CA certificates
- **Example**: `truststore.custom.merge-dir=/etc/myapp/certificates`

### `truststore.custom.fail-on-error`
- **Default**: `false`
- **Description**: Whether to fail the startup initializer, if certificate loading fails. Note that, in this case, the initializer throws an exception but ApplicationContextRunner (which handles all initializers) skips to the next initializer, by default, without exiting the application.
- **Example**: `truststore.custom.fail-on-error=true`

### `truststore.custom.expose-bean`
- **Default**: `false`
- **Description**: Whether to expose `CustomTrustStore` and merged `SSLContext` as Spring beans. Note that, we're not exposing any beans by default, for now. You can enable and use CustomTrustStoreAutoConfiguration.java.disabled for this functionality.
- **Example**: `truststore.custom.expose-bean=true`

## Configuration Examples

### application.yml
```yaml
truststore:
  custom:
    enabled: true
    merge-dir: "truststore/custom/merge"
    fail-on-error: false
    expose-bean: false
```

### application.properties
```properties
truststore.custom.enabled=true
truststore.custom.merge-dir=truststore/custom/merge
truststore.custom.fail-on-error=false
truststore.custom.expose-bean=false
```

## Advanced Usage

### Accessing the CustomTrustStore Bean (When expose-bean=true)

If you enable `expose-bean: true`, you can inject the custom truststore:

```java
@Component
public class MyService {
    private final CustomTrustStore customTrustStore;
    private final SSLContext customSSLContext;
    
    public MyService(CustomTrustStore customTrustStore, SSLContext customSSLContext) {
        this.customTrustStore = customTrustStore;
        this.customSSLContext = customSSLContext;
    }
    
    public void doSomething() {
        // Option 1: Use the pre-merged SSL context (recommended)
        SSLContext sslContext = this.customSSLContext;
        
        // Option 2: Get from CustomTrustStore (will reuse cached result)
        // SSLContext sslContext = customTrustStore.getMergedSSLContext();
        
        // Use the SSL context...
    }
}
```

## Supported Certificate Formats

- `.crt` - Certificate files
- `.cer` - Certificate files  
- `.pem` - PEM encoded certificates

## How It Works

1. **Auto-Detection**: Spring Boot automatically detects and loads this starter
2. **Property Binding**: Uses Spring Boot's `Binder` to automatically map properties
3. **Certificate Loading**: Loads all certificates from the configured directory
4. **Trust Store Merging**: Merges custom certificates with the JRE's default `cacerts`
5. **SSL Context Setup**: Sets the merged trust store as the default SSL context
6. **Application Ready**: Your application now trusts both default and custom certificates

## Expected Logs

When you start the application, you should see:

```
INFO  CustomTrustStoreSpringBootInitializer - CustomTrustStore is enabled. Initializing via ApplicationContextInitializer...
INFO  CustomTrustStoreSpringBootInitializer - Using certificates directory from configuration: truststore/custom/merge
INFO  CustomTrustStore - Merging custom certificates from truststore/custom/merge with original trust store.
INFO  CustomTrustStore - Successfully added certificate: internal-ca.crt
INFO  CustomTrustStore - Successfully added certificate: corporate-ca.cer
INFO  CustomTrustStore - Successfully added certificate: development-ca.pem
INFO  CustomTrustStore - Custom certificates are successfully merged into the new SSLContext.
INFO  CustomTrustStoreSpringBootInitializer - Successfully initialized custom trust store and set as default SSLContext
INFO  SpringApplication - Starting SpringApplication using Java ...
...
```

## Benefits of Each Approach

### Auto-Configuration (Approach 1)
1. **Zero Code**: Works automatically when included
2. **Spring Integration**: Full Spring Boot lifecycle integration
3. **Simple**: Just add dependency and place certificates
4. **Production Ready**: Handles errors gracefully

### ApplicationContextInitializer (Approach 2)
1. **Early Initialization**: Runs before ApplicationContext is created
2. **Spring Integration**: Uses Spring Boot's property binding system
3. **Configuration Support**: Full access to application properties
4. **Bean Ready**: SSL context is ready when beans are created
5. **Standard Pattern**: Follows Spring Boot best practices
6. **Property Binding**: Automatically binds properties to CustomTrustStoreProperties
7. **Environment Access**: Can access all Spring Boot environment properties

### Standalone Usage (Approach 3)
1. **No Dependencies**: Works without Spring Boot
2. **Full Control**: Initialize exactly when you want
3. **Simple Integration**: Just a few lines of code
4. **Framework Agnostic**: Can be used in any Java application

## Troubleshooting

- **No logs?** Check that the dependency (and slf4j dependency with implementation) is properly included
- **Directory not found?** Ensure the certificates directory exists
- **Certificates not loading?** Verify file extensions are `.crt`, `.cer`, or `.pem`
- **SSL errors?** Check that certificates are valid X.509 format
- **Properties not reading?** Ensure property names match exactly (e.g., `truststore.custom.merge-dir`)
- **Initializer not running?** Verify you're calling `app.addInitializers()` before `app.run()`

## Benefits

- ✅ **Multiple Approaches**: Choose the level of control you need
- ✅ **Zero Code Option**: Works automatically when included
- ✅ **No Overwriting**: Preserves original JRE truststore
- ✅ **Flexible**: Configurable via application properties
- ✅ **Spring Integration**: Full Spring Boot lifecycle integration
- ✅ **Standalone Ready**: Can be used without Spring Boot
- ✅ **Production Ready**: Handles errors gracefully
