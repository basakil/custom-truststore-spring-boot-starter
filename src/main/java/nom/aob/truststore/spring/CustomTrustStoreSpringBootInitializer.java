package nom.aob.truststore.spring;

import nom.aob.truststore.CustomTrustStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;

import javax.net.ssl.SSLContext;

/**
 * Spring Boot ApplicationContextInitializer that initializes the custom truststore
 * before the ApplicationContext is created, ensuring SSL context is ready for all beans.
 * 
 * Usage in main method:
 * SpringApplication app = new SpringApplication(MyApplication.class);
 * app.addInitializers(new CustomTrustStoreSpringBootInitializer());
 * app.run(args);
 */
public class CustomTrustStoreSpringBootInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CustomTrustStoreSpringBootInitializer.class);
    private static final String DEFAULT_MERGE_DIR = "truststore/custom/merge";

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {

        // Get the environment to access properties
        ConfigurableEnvironment environment = applicationContext.getEnvironment();

        // Bind properties to CustomTrustStoreProperties using Spring Boot's Binder
        CustomTrustStoreProperties properties = Binder.get(environment)
            .bind("truststore.custom", Bindable.of(CustomTrustStoreProperties.class))
            .orElseGet(() -> {
                // Create default properties if none are configured
                CustomTrustStoreProperties defaultProps = new CustomTrustStoreProperties();
                defaultProps.setMergeDir("");
                defaultProps.setFailOnError(false);
                defaultProps.setEnabled(true);
                return defaultProps;
            });

        // Check if enabled
        if (!properties.isEnabled()) {
            LOGGER.info("CustomTrustStore is disabled via configuration");
            return;
        } else {
            LOGGER.info("CustomTrustStore is enabled. Initializing via ApplicationContextInitializer...");
        }

        // Determine the certificates directory path
        String certsDirPath = properties.getMergeDir();
        if (certsDirPath == null || certsDirPath.trim().isEmpty()) {
            certsDirPath = DEFAULT_MERGE_DIR;
            LOGGER.info("Using default certificates directory: {}", certsDirPath);
        } else {
            LOGGER.info("Using certificates directory from configuration: {}", certsDirPath);
        }

        try {
            // Create and initialize the custom trust store
            CustomTrustStore customTrustStore = new CustomTrustStore(certsDirPath);
            SSLContext mergedSSLContext = customTrustStore.getMergedSSLContext();
            
            if (mergedSSLContext != null) {
                SSLContext.setDefault(mergedSSLContext);
                LOGGER.info("Successfully initialized custom trust store and set as default SSLContext");
            } else {
                throw new IllegalStateException("CustomTrustStore.getMergedSSLContext() returned null");
            }
            
        } catch (Exception ex) {
            // Check if we should fail on error
            boolean failOnError = properties.isFailOnError();
            
            if (failOnError) {
                LOGGER.error("Failed to initialize custom trust store. Application will exit due to fail-on-error=true property", ex);
                throw new RuntimeException("Failed to initialize custom trust store", ex);
            } else {
                LOGGER.error("Failed to initialize custom trust store. Continuing with default trust store.", ex);
            }
        }
    }
}
