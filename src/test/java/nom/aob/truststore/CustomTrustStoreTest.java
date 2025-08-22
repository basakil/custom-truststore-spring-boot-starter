package nom.aob.truststore;

import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsServer;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.net.ssl.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * An integration test suite for the CustomTrustStore class.
 * This test class simulates a real-world scenario by programmatically
 * generating a self-signed certificate, starting a temporary HTTPS server
 * with that certificate, and then testing a client's ability to connect to it.
 */
public class CustomTrustStoreTest {

    private Path tempCustomCertDir;
    private HttpsServer server;
    private String serverUrl;

    /**
     * Set up a temporary directory, generate a self-signed certificate,
     * and start a temporary HTTPS server before each test.
     */
    @BeforeEach
    public void setup() throws Exception {
        // Create a temporary directory to act as our custom certs directory.
        this.tempCustomCertDir = Files.createTempDirectory("custom-certs-test");

        // 1. Generate a test certificate and a key pair for the mock server.
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        KeyPair keyPair = keyPairGenerator.generateKeyPair();

        X500Name issuer = new X500Name("CN=Test Self-Signed CA, O=Example, L=Test, C=US");
        X500Name subject = new X500Name("CN=localhost"); // Server will be on localhost
        Date notBefore = new Date(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(1));
        Date notAfter = new Date(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(365));
        BigInteger serial = BigInteger.valueOf(System.currentTimeMillis());

        ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA").build(keyPair.getPrivate());
        X509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(
                issuer, serial, notBefore, notAfter, subject, keyPair.getPublic());

        X509Certificate testCertificate = new JcaX509CertificateConverter().getCertificate(builder.build(signer));

        // Save the generated certificate to a file. This is what the client (the test) will use.
        File tempCertFile = this.tempCustomCertDir.resolve("test-cert.crt").toFile();
        try (FileOutputStream fos = new FileOutputStream(tempCertFile)) {
            fos.write(testCertificate.getEncoded());
        }

        // 2. Configure and start the temporary HTTPS server.
        KeyStore serverKeyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        serverKeyStore.load(null, null);
        serverKeyStore.setKeyEntry("server-key", keyPair.getPrivate(), "changeit".toCharArray(),
                new java.security.cert.Certificate[]{testCertificate});

        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(serverKeyStore, "changeit".toCharArray());

        SSLContext serverSslContext = SSLContext.getInstance("TLS");
        serverSslContext.init(kmf.getKeyManagers(), null, null);

        this.server = HttpsServer.create(new InetSocketAddress("localhost", 0), 0);
        this.server.setHttpsConfigurator(new HttpsConfigurator(serverSslContext));
        this.server.createContext("/", exchange -> {
            String response = "Success!";
            exchange.sendResponseHeaders(200, response.length());
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes());
            os.close();
        });
        this.server.start();

        this.serverUrl = "https://localhost:" + this.server.getAddress().getPort();
    }

    /**
     * Clean up the temporary directory and files after each test.
     */
    @AfterEach
    public void teardown() throws IOException {
        if (this.server != null) {
            this.server.stop(0); // Stop the server immediately.
        }
        if (this.tempCustomCertDir != null) {
            Files.walk(this.tempCustomCertDir)
                    .sorted((p1, p2) -> -p1.compareTo(p2))
                    .map(Path::toFile)
                    .forEach(File::delete);
        }
    }

    @Test
    public void testSslConnection_failsWithoutCustomTrustStore() {
        // Arrange
        // The JVM's default trust store does not trust our self-signed certificate.

        // Act & Assert
        // We expect an SSLHandshakeException when trying to connect to the mock server.
        assertThrows(SSLHandshakeException.class, () -> {
            URL url = new URL(serverUrl);
            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
            // Set timeouts to prevent the test from hanging indefinitely.
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.connect(); // This will trigger the SSL handshake.
        }, "Connection should fail due to untrusted certificate.");
    }

    @Test
    public void testSslConnection_succeedsWithCustomTrustStore() {
        // Arrange
        // Get the merged SSLContext with the custom certificate added.
        CustomTrustStore customTrustStore = new CustomTrustStore(tempCustomCertDir.toString());
        SSLContext mergedSslContext = customTrustStore.getMergedSSLContext();

        // Store the original SSLContext to restore it later
        SSLContext originalSslContext = null;
        HttpsURLConnection conn = null;
        try {
            originalSslContext = SSLContext.getDefault();
            // Set the merged context as the JVM's default for the duration of the test.
            // This is the most reliable way to ensure the client uses the correct trust store.
            SSLContext.setDefault(mergedSslContext);

            // Set up the connection and its properties
            URL url = new URL(serverUrl);
            conn = (HttpsURLConnection) url.openConnection();

            conn.setSSLSocketFactory(mergedSslContext.getSocketFactory());
            // Add a permissive hostname verifier for localhost to prevent hostname errors.
            conn.setHostnameVerifier(new HostnameVerifier() {
                @Override
                public boolean verify(String hostname, SSLSession session) {
                    return hostname.equalsIgnoreCase("localhost");
                }
            });

            // Set timeouts to prevent the test from hanging indefinitely.
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            // Act
            conn.connect();

            // Assert
            int responseCode = conn.getResponseCode();
            assertEquals(200, responseCode, "Expected a successful connection and a 200 response code.");
        } catch (Exception e) {
            // Re-throw any exceptions to ensure the test fails if something unexpected happens.
            throw new RuntimeException("Test failed with an unexpected exception.", e);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
            // Always restore the original SSLContext after the test is complete.
            if (originalSslContext != null) {
                try {
                    SSLContext.setDefault(originalSslContext);
                } catch (Exception e) {
                    System.err.println("Failed to restore original SSLContext: " + e.getMessage());
                }
            }
        }
    }

    @Test
    public void testGetMergedSSLContext_withInvalidPath() {
        // Arrange
        String invalidPath = "/this/path/does/not/exist";

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            new CustomTrustStore(invalidPath).getMergedSSLContext();
        }, "Should throw IllegalArgumentException for an invalid path.");
    }
}
