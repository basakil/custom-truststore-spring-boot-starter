package nom.aob.spring.customTrustStoreSpringBootStarter;

import nom.aob.trustStore.CustomTrustStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;

import javax.net.ssl.SSLContext;
import java.io.File;

/**
 * Initializer that checks and loads additional trusted (CA) certificates from a directory.
 * Uses "trustStore.custom" prefixed properties.
 *
 * @see CustomTrustStore
 */
public class CustomTrustStoreSpringBootStarter implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CustomTrustStoreSpringBootStarter.class.getName());

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {

        // load properties:
        Environment env = applicationContext.getEnvironment();
        String certsDirPath = env.getProperty("trustStore.custom.dir");
        String exitOnErrorStr = env.getProperty("trustStore.custom.fail-on-error");
        boolean exitOnError = Boolean.parseBoolean(exitOnErrorStr); // this makes the default as FALSE.

        if (certsDirPath == null || certsDirPath.trim().isEmpty()) {
            certsDirPath = new File("trustStore/custom").getAbsolutePath();
            LOGGER.info("Using default certs directory: " + certsDirPath);
        } else {
            LOGGER.info("Using certs directory {} from property trustStore.custom.dir .", certsDirPath);
        }

        CustomTrustStore customTrustStore = new CustomTrustStore(certsDirPath);
        try {
            SSLContext mergedSSLContext = customTrustStore.getMergedSSLContext();
            if (mergedSSLContext != null) {
                SSLContext.setDefault(mergedSSLContext);
            } else {
                throw new IllegalStateException("returned null from CustomTrustStore.getMergedSSLContext().");
            }
        } catch (Throwable ex) {
            if (exitOnError) {
                throw ex;
            } else {
                LOGGER.error("Exception caught while trying to load certificates from" + certsDirPath, ex);
            }
        }
    }

}
