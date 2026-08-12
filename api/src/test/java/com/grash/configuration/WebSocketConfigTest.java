package com.grash.configuration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.messaging.simp.broker.SimpleBrokerMessageHandler;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.support.ExecutorSubscribableChannel;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.socket.config.annotation.SockJsServiceRegistration;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.StompWebSocketEndpointRegistration;

import java.util.Collection;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WebSocketConfigTest {

    private static class ExposedMessageBrokerRegistry extends MessageBrokerRegistry {
        ExposedMessageBrokerRegistry() {
            super(new ExecutorSubscribableChannel(), new ExecutorSubscribableChannel());
        }

        Collection<String> getApplicationPrefixes() {
            return getApplicationDestinationPrefixes();
        }

        SimpleBrokerMessageHandler getBrokerHandler(SubscribableChannel channel) {
            return getSimpleBroker(channel);
        }
    }

    private final WebSocketConfig webSocketConfig = new WebSocketConfig();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(webSocketConfig, "frontendUrl", "http://localhost:3000");
    }

    @Test
    void configureMessageBroker_setsBrokerAndApplicationPrefixes() {
        ExposedMessageBrokerRegistry registry = new ExposedMessageBrokerRegistry();

        webSocketConfig.configureMessageBroker(registry);

        Collection<String> applicationPrefixes = registry.getApplicationPrefixes();
        assertEquals(1, applicationPrefixes.size());
        assertTrue(applicationPrefixes.contains("/app"));

        Collection<String> brokerPrefixes = registry.getBrokerHandler(new ExecutorSubscribableChannel())
                .getDestinationPrefixes();
        assertEquals(4, brokerPrefixes.size());
        assertTrue(brokerPrefixes.contains("/notifications"));
        assertTrue(brokerPrefixes.contains("/exports"));
        assertTrue(brokerPrefixes.contains("/imports"));
        assertTrue(brokerPrefixes.contains("/user"));
    }

    @Test
    void registerStompEndpoints_registersWsEndpointWithSockJs() {
        StompEndpointRegistry registry = mock(StompEndpointRegistry.class);
        StompWebSocketEndpointRegistration endpointRegistration = mock(StompWebSocketEndpointRegistration.class);
        SockJsServiceRegistration sockJsRegistration = mock(SockJsServiceRegistration.class);
        when(registry.addEndpoint("/ws")).thenReturn(endpointRegistration);
        when(endpointRegistration.setAllowedOrigins("http://localhost:3000")).thenReturn(endpointRegistration);
        when(endpointRegistration.withSockJS()).thenReturn(sockJsRegistration);

        assertDoesNotThrow(() -> webSocketConfig.registerStompEndpoints(registry));

        verify(registry).addEndpoint("/ws");
        verify(endpointRegistration).setAllowedOrigins("http://localhost:3000");
        verify(endpointRegistration).withSockJS();
    }
}
