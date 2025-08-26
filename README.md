# Custom Trust Store Spring Boot Starter

A Spring Boot starter that automatically merges custom CA certificates with the JRE's default trust store during application startup. This allows applications to trust custom certificates without modifying the system's default trust store.

## Features

- **Automatic Integration**: Works automatically when included as a dependency - no manual configuration required
- **Early Initialization**: Runs before most Spring beans are created, ensuring SSL context is ready for all components
- **Flexible Configuration**: Configurable certificate directory and error handling behavior
- **Non-Destructive**: Preserves the original JRE trust store while adding custom certificates
- **Runtime Merging**: Certificates are merged at runtime, not at build time

## Usage

### 1. Add Dependency

Include this starter in your Spring Boot project's `pom.xml`:

```xml
<dependency>
    <groupId>nom.aob.spring</groupId>
    <artifactId>custom-truststore-spring-boot-starter</artifactId>
    <version>1.0.7</version>
</dependency>
```

### 2. Place Custom Certificates

Place your custom CA certificates (`.crt`, `.cer`, or `.pem` files) in one of these locations:

- **Default**: `truststore/custom/merge/` directory in your project root
- **Custom**: Configure using `truststore.custom.merge-dir` property

### 3. Configuration (Optional)

You can customize the behavior using these properties in your `application.yml` or `application.properties`:

```yaml
truststore:
  custom:
    enabled: true                    # Enable/disable the starter (default: true)
    merge-dir: /path/to/certs       # Custom certificates directory
    fail-on-error: false            # Whether to fail startup on error (default: false)
    expose-bean: false              # Expose CustomTrustStore as Spring bean (default: false)
```

## How It Works

1. **Auto-Detection**: Spring Boot automatically detects and loads this starter
2. **Early Initialization**: The starter initializes during the `InitializingBean.afterPropertiesSet()` phase
3. **Certificate Loading**: Loads all certificates from the configured directory
4. **Trust Store Merging**: Merges custom certificates with the JRE's default `cacerts`
5. **SSL Context Setup**: Sets the merged trust store as the default SSL context
6. **Application Ready**: Your application now trusts both default and custom certificates

## Example

### Basic Usage (No Configuration Required)

```java
@SpringBootApplication
public class MyApplication {
    public static void main(String[] args) {
        SpringApplication.run(MyApplication.class, args);
    }
}
```

Just include the dependency and place certificates in `truststore/custom/merge/` - that's it!

### Custom Configuration

```yaml
# application.yml
truststore:
  custom:
    merge-dir: /etc/myapp/certificates
    fail-on-error: true
    expose-bean: true
```

### Accessing the CustomTrustStore Bean

If you enable `expose-bean: true`, you can inject the `CustomTrustStore` or the pre-merged `SSLContext`:

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

**Note**: The starter is optimized to avoid duplicate certificate merging. Both beans return the same pre-initialized instances, ensuring certificates are only loaded and merged once during startup.

## Certificate File Types

The starter supports these certificate file formats:
- `.crt` - Certificate files
- `.cer` - Certificate files  
- `.pem` - PEM encoded certificates

## Error Handling

- **Default Behavior**: If certificate loading fails, the application continues with the default trust store
- **Fail on Error**: Set `truststore.custom.fail-on-error=true` to make startup fail if certificate loading fails
- **Logging**: All operations are logged with appropriate log levels

## Requirements

- Java 8 or higher
- Spring Boot 2.x or 3.x
- Certificates must be valid X.509 format

## Troubleshooting

### Certificates Not Loading

1. Check the directory path in logs
2. Ensure certificates have proper file extensions (`.crt`, `.cer`, `.pem`)
3. Verify file permissions
4. Check for certificate format errors in logs

### SSL Context Not Updated

1. Verify the starter is being loaded (check startup logs)
2. Ensure no other code is overriding the SSL context after startup
3. Check that certificates are being loaded successfully

### Application Fails to Start

1. Set `truststore.custom.fail-on-error=false` to continue on errors
2. Check certificate directory exists and is accessible
3. Verify certificate files are valid X.509 format

## License

This project is licensed under the same terms as your project.
