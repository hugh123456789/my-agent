package com.agent.codeagent.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ModelPropertiesTest {

    @Test
    void storesSelectedProviderConfiguration() {
        ModelProperties properties = new ModelProperties();
        properties.setProvider("deepseek");

        ModelProperties.Provider deepseek = new ModelProperties.Provider();
        deepseek.setBaseUrl("https://api.deepseek.com");
        deepseek.setApiKey("secret");
        deepseek.setModelName("deepseek-v4-flash");
        properties.getModels().put("deepseek", deepseek);

        assertEquals("deepseek", properties.getProvider());
        assertEquals("https://api.deepseek.com", properties.provider("deepseek").getBaseUrl());
        assertEquals("secret", properties.provider("deepseek").getApiKey());
        assertEquals("deepseek-v4-flash", properties.provider("deepseek").getModelName());
    }
}
