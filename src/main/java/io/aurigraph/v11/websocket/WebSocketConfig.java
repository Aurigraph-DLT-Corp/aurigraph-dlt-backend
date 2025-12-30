package io.aurigraph.v11.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import org.jboss.logging.Logger;

/**
 * WebSocket configuration
 * Provides beans for WebSocket infrastructure
 */
@ApplicationScoped
public class WebSocketConfig {

    private static final Logger LOG = Logger.getLogger(WebSocketConfig.class);

    /**
     * Provide ObjectMapper for JSON serialization
     * Configured for WebSocket message serialization
     */
    @Produces
    @Singleton
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();

        // Register Java 8 Time module for Instant serialization
        mapper.registerModule(new JavaTimeModule());

        // Disable writing dates as timestamps
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // Pretty print disabled for WebSocket bandwidth optimization
        mapper.disable(SerializationFeature.INDENT_OUTPUT);

        LOG.info("ObjectMapper configured for WebSocket message serialization");

        return mapper;
    }
}
