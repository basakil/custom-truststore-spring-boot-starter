package nom.aob.truststore.spring;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for CustomTrustStore.
 * 
 * Properties can be configured in application.yml/properties with the prefix "truststore.custom"
 */
@ConfigurationProperties(prefix = "truststore.custom")
public class CustomTrustStoreProperties {

    /**
     * Whether to enable the custom trust store functionality.
     * Default: true
     */
    private boolean enabled = true;

    /**
     * Directory path containing custom certificates to merge.
     * Default: "truststore/custom/merge" (relative to working directory)
     */
    private String mergeDir;

    /**
     * Whether to fail the application startup if trust store initialization fails.
     * Default: false
     */
    private boolean failOnError = false;

    /**
     * Whether to expose the CustomTrustStore as a Spring bean.
     * Default: false
     */
    private boolean exposeBean = false;

    // Getters and Setters
    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getMergeDir() {
        return mergeDir;
    }

    public void setMergeDir(String mergeDir) {
        this.mergeDir = mergeDir;
    }

    public boolean isFailOnError() {
        return failOnError;
    }

    public void setFailOnError(boolean failOnError) {
        this.failOnError = failOnError;
    }

    public boolean isExposeBean() {
        return exposeBean;
    }

    public void setExposeBean(boolean exposeBean) {
        this.exposeBean = exposeBean;
    }
}
