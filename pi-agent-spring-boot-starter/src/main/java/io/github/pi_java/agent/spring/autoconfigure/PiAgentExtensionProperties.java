package io.github.pi_java.agent.spring.autoconfigure;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pi.extensions")
public class PiAgentExtensionProperties {

    private boolean enabled = true;
    private String platformApiVersion = "1.0.0";
    private List<String> disabledSources = new ArrayList<>();
    private List<String> disabledCapabilities = new ArrayList<>();
    private boolean allowDuplicateCapabilityOverrides;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getPlatformApiVersion() {
        return platformApiVersion;
    }

    public void setPlatformApiVersion(String platformApiVersion) {
        this.platformApiVersion = platformApiVersion;
    }

    public List<String> getDisabledSources() {
        return disabledSources;
    }

    public void setDisabledSources(List<String> disabledSources) {
        this.disabledSources = disabledSources == null ? new ArrayList<>() : new ArrayList<>(disabledSources);
    }

    public List<String> getDisabledCapabilities() {
        return disabledCapabilities;
    }

    public void setDisabledCapabilities(List<String> disabledCapabilities) {
        this.disabledCapabilities = disabledCapabilities == null ? new ArrayList<>() : new ArrayList<>(disabledCapabilities);
    }

    public boolean isAllowDuplicateCapabilityOverrides() {
        return allowDuplicateCapabilityOverrides;
    }

    public void setAllowDuplicateCapabilityOverrides(boolean allowDuplicateCapabilityOverrides) {
        this.allowDuplicateCapabilityOverrides = allowDuplicateCapabilityOverrides;
    }
}
