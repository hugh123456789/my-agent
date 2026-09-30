package com.agent.codeagent.agents.hook.core;

import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Locale;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/** 维护事件到有序回调列表的映射，并负责统一触发。 */
@Component
public class HookRegistry {

    private static final Logger log = LoggerFactory.getLogger(HookRegistry.class);

    private final Map<HookEvent, List<HookCallback>> callbacks = new ConcurrentHashMap<>();

    @Autowired
    public HookRegistry(List<HookRegistrar> registrars,
                        @Value("${agent.hooks.enabled:}") String enabledHooks) {
        this(registrars, parseEnabledHooks(enabledHooks));
    }

    public HookRegistry(List<HookRegistrar> registrars) {
        this(registrars, registrars == null
                ? Set.of()
                : registrars.stream().map(HookRegistrar::name).collect(java.util.stream.Collectors.toSet()));
    }

    public HookRegistry(List<HookRegistrar> registrars, Collection<String> enabledHookNames) {
        Set<String> enabled = resolveEnabledNames(registrars, enabledHookNames);
        if (registrars != null) {
            registrars.stream()
                    .filter(registrar -> enabled.contains(registrar.name().toLowerCase(Locale.ROOT)))
                    .forEach(registrar -> registrar.register(this));
        }
    }

    private static Set<String> parseEnabledHooks(String enabledHooks) {
        if (enabledHooks == null || enabledHooks.isBlank()) return null;
        return Set.of(enabledHooks.split(","));
    }

    private static Set<String> resolveEnabledNames(
            List<HookRegistrar> registrars,
            Collection<String> enabledHookNames) {
        if (enabledHookNames == null) {
            if (registrars == null) return Set.of();
            return registrars.stream()
                    .map(HookRegistrar::name)
                    .map(name -> name.toLowerCase(Locale.ROOT))
                    .collect(java.util.stream.Collectors.toUnmodifiableSet());
        }
        return enabledHookNames.stream()
                .filter(name -> name != null && !name.isBlank())
                .map(name -> name.trim().toLowerCase(Locale.ROOT))
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    public void registerHook(HookEvent event, HookCallback callback) {
        if (event == null) {
            throw new IllegalArgumentException("event must not be null");
        }
        if (callback == null) {
            throw new IllegalArgumentException("callback must not be null");
        }
        callbacks.computeIfAbsent(event, ignored -> new CopyOnWriteArrayList<>()).add(callback);
    }

    public void registerHook(String eventName, HookCallback callback) {
        registerHook(HookEvent.fromValue(eventName), callback);
    }

    public Object triggerHooks(HookEvent event, Object block, Object output) {
        return triggerHooks(event, null, block, output);
    }

    public Object triggerHooks(HookEvent event, String sessionId, Object block, Object output) {
        if (event == null) {
            throw new IllegalArgumentException("event must not be null");
        }
        Object current = output;
        for (HookCallback callback : callbacks.getOrDefault(event, List.of())) {
            try {
                Object next = callback.apply(new HookContext(event, block, current, sessionId));
                if (next != null) {
                    current = next;
                }
            } catch (Exception error) {
                log.warn("Hook callback failed for event {} ({}); continuing with later hooks",
                        event.value(), callback.getClass().getName(), error);
            }
        }
        return current;
    }

    public Object triggerHooks(String eventName, Object block, Object output) {
        return triggerHooks(HookEvent.fromValue(eventName), block, output);
    }
}
