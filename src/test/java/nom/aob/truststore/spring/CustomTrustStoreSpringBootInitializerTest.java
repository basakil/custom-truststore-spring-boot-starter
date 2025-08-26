package nom.aob.truststore.spring;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Simple test class for CustomTrustStoreSpringBootInitializer.
 */
public class CustomTrustStoreSpringBootInitializerTest {

    @Test
    public void testInitializerCanBeInstantiated() {
        // Test that the initializer can be created without errors
        CustomTrustStoreSpringBootInitializer initializer = new CustomTrustStoreSpringBootInitializer();
        assertNotNull(initializer);
    }

    @Test
    public void testInitializerImplementsCorrectInterface() {
        // Test that the initializer implements the correct interface
        CustomTrustStoreSpringBootInitializer initializer = new CustomTrustStoreSpringBootInitializer();
        assertTrue(initializer instanceof org.springframework.context.ApplicationContextInitializer);
    }
}
