package com.agent.codeagent.common;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import java.util.Locale;

@Configuration
@EnableConfigurationProperties(ModelProperties.class)
public class ModelConfig {

    @Bean("mainChatModel")
    @Lazy
    @ConditionalOnProperty(name = "agent.provider", matchIfMissing = true)
    public ChatModel mainChatModel(ModelProperties properties) {
        return chatModel(selected(properties));
    }

    @Bean("mainStreamingChatModel")
    @Lazy
    @ConditionalOnProperty(name = "agent.provider", matchIfMissing = true)
    public StreamingChatModel mainStreamingChatModel(ModelProperties properties) {
        return streamingModel(selected(properties));
    }

    private ModelProperties.Provider selected(ModelProperties properties) {
        ModelProperties.Provider provider = properties.provider(
                properties.getProvider().toLowerCase(Locale.ROOT));
        if (!provider.isEnabled()) {
            throw new IllegalStateException("Selected model provider is disabled: "
                    + properties.getProvider());
        }
        return provider;
    }

    private ChatModel chatModel(ModelProperties.Provider provider) {
        return OpenAiChatModel.builder()
                .baseUrl(provider.getBaseUrl())
                .apiKey(provider.getApiKey())
                .modelName(provider.getModelName())
                .returnThinking(provider.isReturnThinking())
                .logRequests(true)
                .logResponses(true)
                .build();
    }

    private StreamingChatModel streamingModel(ModelProperties.Provider provider) {
        return OpenAiStreamingChatModel.builder()
                .baseUrl(provider.getBaseUrl())
                .apiKey(provider.getApiKey())
                .modelName(provider.getModelName())
                .returnThinking(provider.isReturnThinking())
                .reasoningEffort("low")
                .logRequests(true)
                .logResponses(true)
                .build();
    }
}
