package io.devforge.platform.core.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocalSecurityConfigurationTests {

    @Test
    void acceptsOnlyLoopbackBindingsForUnauthenticatedLocalProfile() {
        assertTrue(LocalSecurityConfiguration.isLoopbackBinding("127.0.0.1"));
        assertTrue(LocalSecurityConfiguration.isLoopbackBinding("::1"));
        assertTrue(LocalSecurityConfiguration.isLoopbackBinding("localhost"));
        assertFalse(LocalSecurityConfiguration.isLoopbackBinding("0.0.0.0"));
        assertFalse(LocalSecurityConfiguration.isLoopbackBinding("::"));
        assertFalse(LocalSecurityConfiguration.isLoopbackBinding("192.0.2.10"));
        assertFalse(LocalSecurityConfiguration.isLoopbackBinding("not-a-host"));
    }
}
