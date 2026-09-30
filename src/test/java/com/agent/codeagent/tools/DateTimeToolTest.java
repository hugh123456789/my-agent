package com.agent.codeagent.tools;

import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Component;

import static org.junit.jupiter.api.Assertions.assertTrue;

class DateTimeToolTest {

    @Test
    void isManagedAsSpringComponent() {
        assertTrue(DateTimeTool.class.isAnnotationPresent(Component.class));
    }
}
