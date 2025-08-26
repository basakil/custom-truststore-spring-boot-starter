# Custom TrustStore Spring Boot Starter - Usage Examples

## Option 1: Using ApplicationContextInitializer (Recommended)

The `CustomTrustStoreSpringBootInitializer` provides the most control over when the custom truststore is initialized. It runs before the ApplicationContext is created, ensuring SSL context is ready for all beans.

### Basic Usage in Main Method

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

### Alternative: Using SpringApplicationBuilder

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

### Configuration in application.yml

```yaml
truststore:
  custom:
    enabled: true
    merge-dir: "truststore/custom/merge"
    fail-on-error: false
    expose-bean: false
```

### Configuration in application.properties

```properties
truststore.custom.enabled=true
truststore.custom.merge-dir=truststore/custom/merge
truststore.custom.fail-on-error=false
truststore.custom.expose-bean=false
```

## Option 2: Using the Static Initializer

The static initializer approach gives you full control over when the custom truststore is initialized. You can call it directly in your main method before Spring Boot starts.

### Basic Usage

```java
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

### Advanced Usage with Custom Configuration

```java
import nom.aob.truststore.spring.CustomTrustStoreInitializer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class MyApplication {

    public static void main(String[] args) {
        // Initialize with custom directory and fail-on-error setting
        CustomTrustStoreInitializer.initialize("custom/certs", true);
        
        // Now start Spring Boot
        SpringApplication.run(MyApplication.class, args);
    }
}
```

### Using with Properties Object

```java
import nom.aob.truststore.spring.CustomTrustStoreInitializer;
import nom.aob.truststore.spring.CustomTrustStoreProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class MyApplication {

    public static void main(String[] args) {
        // Create properties object
        CustomTrustStoreProperties properties = new CustomTrustStoreProperties();
        properties.setMergeDir("custom/certs");
        properties.setFailOnError(true);
        
        // Initialize using properties
        CustomTrustStoreInitializer.initialize(properties);
        
        // Now start Spring Boot
        SpringApplication.run(MyApplication.class, args);
    }
}
```

## Option 3: Using Auto-Configuration (Least Control)

The auto-configuration approach works automatically when included as a dependency, but provides the least control over initialization order.

### Enable in application.yml

```yaml
truststore:
  custom:
    enabled: true
    merge-dir: "truststore/custom/merge"
    fail-on-error: false
    expose-bean: false
```

### Enable in application.properties

```properties
truststore.custom.enabled=true
truststore.custom.merge-dir=truststore/custom/merge
truststore.custom.fail-on-error=false
truststore.custom.expose-bean=false
```

## Configuration Properties

| Property | Default | Description |
|----------|---------|-------------|
| `truststore.custom.enabled` | `true` | Whether to enable the custom trust store functionality |
| `truststore.custom.merge-dir` | `"truststore/custom/merge"` | Directory path containing custom certificates to merge |
| `truststore.custom.fail-on-error` | `false` | Whether to fail the application startup if trust store initialization fails |
| `truststore.custom.expose-bean` | `false` | Whether to expose the CustomTrustStore as a Spring bean |

## Directory Structure

Place your custom certificates in the merge directory:

```
your-app/
├── truststore/
│   └── custom/
│       └── merge/
│           ├── custom-cert-1.crt
│           ├── custom-cert-2.cer
│           └── custom-cert-3.pem
├── src/
└── pom.xml
```

## Supported Certificate Formats

- `.crt` - Certificate files
- `.cer` - Certificate files  
- `.pem` - PEM encoded certificates

## Benefits of Each Approach

### ApplicationContextInitializer (Recommended)
1. **Early Initialization**: Runs before ApplicationContext is created
2. **Spring Integration**: Uses Spring Boot's property binding system
3. **Configuration Support**: Full access to application properties
4. **Bean Ready**: SSL context is ready when beans are created
5. **Standard Pattern**: Follows Spring Boot best practices

### Static Initializer
1. **Full Control**: Initialize before any Spring code runs
2. **Simple Integration**: Just one line of code in main method
3. **Predictable Order**: No dependency on Spring Boot's lifecycle
4. **Easy Debugging**: Clear initialization point in the code

### Auto-Configuration
1. **Zero Code**: Works automatically when included
2. **Spring Integration**: Full Spring Boot lifecycle integration
3. **Less Control**: Depends on Spring Boot's bean initialization order

## Troubleshooting

- **No logs?** Check that the dependency is properly included
- **Directory not found?** Ensure the certificates directory exists
- **Certificates not loading?** Verify file extensions are `.crt`, `.cer`, or `.pem`
- **SSL errors?** Check that certificates are valid X.509 format
- **Properties not reading?** Ensure property names match exactly (e.g., `truststore.custom.merge-dir`)
