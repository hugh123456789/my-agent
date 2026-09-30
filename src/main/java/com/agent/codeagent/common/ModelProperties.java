package com.agent.codeagent.common;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.LinkedHashMap;
import java.util.Map;

/** Typed model configuration with explicit provider selection. */
@ConfigurationProperties(prefix = "agent")
public class ModelProperties {

    private String provider = "deepseek";
    private Map<String, Provider> models = new LinkedHashMap<>();

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public Map<String, Provider> getModels() { return models; }
    public void setModels(Map<String, Provider> models) { this.models = models; }

    public Provider provider(String name) {
        Provider provider = models.get(name);
        if (provider == null) {
            throw new IllegalArgumentException("No model configuration for provider: " + name);
        }
        return provider;
    }

    public static class Provider {
        private boolean enabled;
        private String baseUrl;
        private String apiKey;
        private String modelName;
        private boolean returnThinking;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
        public String getModelName() { return modelName; }
        public void setModelName(String modelName) { this.modelName = modelName; }
        public boolean isReturnThinking() { return returnThinking; }
        public void setReturnThinking(boolean returnThinking) { this.returnThinking = returnThinking; }
    }
}
