package nom.aob.trustStore;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A utility class to create an SSLContext by merging custom trust certificates
 * from a directory with an existing trust store, such as the default JRE 'cacerts'.
 * This prevents overwriting the default trusted certificates.
 */
public class CustomTrustStore {

    private static final Logger LOGGER = LoggerFactory.getLogger(CustomTrustStore.class);
    private static final String DEFAULT_TRUSTSTORE_PASSWORD = "changeit";

    private final String extendedTruststorePath;
    private final String originalTruststorePath;
    private final String originalTruststorePassword;

    /**
     * Constructs a CustomTrustStore instance that will merge custom certificates
     * with the default JRE 'cacerts' trust store.
     *
     * @param extendedTruststorePath The path to the directory containing custom certificates.
     */
    public CustomTrustStore(String extendedTruststorePath) {
        this.extendedTruststorePath = extendedTruststorePath;
        this.originalTruststorePath = System.getProperty("java.home") + File.separator + "lib" + File.separator + "security" + File.separator + "cacerts";
        this.originalTruststorePassword = DEFAULT_TRUSTSTORE_PASSWORD;
    }

    /**
     * Constructs a CustomTrustStore instance that will merge custom certificates
     * with a specified original trust store.
     *
     * @param extendedTruststorePath The path to the directory containing custom certificates.
     * @param originalTruststorePath The path to the original trust store file.
     * @param originalTruststorePassword The password for the original trust store.
     */
    public CustomTrustStore(String extendedTruststorePath,
                            String originalTruststorePath,
                            String originalTruststorePassword) {

        this.extendedTruststorePath = extendedTruststorePath;
        this.originalTruststorePath = originalTruststorePath;
        this.originalTruststorePassword = originalTruststorePassword;
    }

    /**
     * Creates and returns an SSLContext by merging custom certificates with the original trust store.
     *
     * @return A new SSLContext configured with both original and custom certificates.
     * @throws IllegalStateException if the trust store configuration fails.
     */
    public SSLContext getMergedSSLContext() {
        File certDirectory = new File(this.extendedTruststorePath);
        if (!certDirectory.exists() || !certDirectory.isDirectory()) {
            LOGGER.warn("Custom trust store directory not found or is not a directory: {}", this.extendedTruststorePath);
            throw new IllegalArgumentException("Invalid custom trust store directory path.");
        }

        try {
            LOGGER.info("Merging custom certificates from {} with original trust store.", this.extendedTruststorePath);

            // 1. Load the original trust store (e.g., JRE 'cacerts').
            KeyStore originalTrustStore = KeyStore.getInstance(KeyStore.getDefaultType());
            try (InputStream is = new FileInputStream(this.originalTruststorePath)) {
                originalTrustStore.load(is, this.originalTruststorePassword.toCharArray());
            }

            // 2. Load and add custom certificates to the original trust store.
            CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
            File[] certFiles = certDirectory.listFiles(
                    (dir, name) -> name.endsWith(".crt") || name.endsWith(".cer") || name.endsWith(".pem")
            );

            if (certFiles != null) {
                for (File certFile : certFiles) {
                    try (InputStream certStream = new FileInputStream(certFile)) {
                        Certificate cert = certFactory.generateCertificate(certStream);
                        // Add the certificate to the KeyStore. The alias is the filename without extension.
                        originalTrustStore.setCertificateEntry(certFile.getName().split("\\.")[0], cert);
                        LOGGER.info("Successfully added certificate: {}", certFile.getName());
                    } catch (Exception e) {
                        LOGGER.warn("Failed to add certificate {}: {}", certFile.getName(), e.getMessage());
                    }
                }
            } else {
                LOGGER.info("No certificate files found in {}.", this.extendedTruststorePath);
            }

            // 3. Initialize a TrustManagerFactory with the combined KeyStore.
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(originalTrustStore);

            // 4. Create a new SSLContext using the merged TrustManagers.
            SSLContext sslContext = SSLContext.getInstance("TLS");
            TrustManager[] trustManagers = tmf.getTrustManagers();
            sslContext.init(null, trustManagers, null);

            LOGGER.info("Custom certificates are successfully merged into the new SSLContext.");
            return sslContext;

        } catch (Exception e) {
            LOGGER.error("Fatal error during trust store configuration: {}", e.getMessage());
            throw new IllegalStateException("Failed to configure trust store at startup.", e);
        }
    }
}
