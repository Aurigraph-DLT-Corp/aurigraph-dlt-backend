package io.aurigraph.v11.bridge.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * JPA converter for serializing/deserializing Map<String, String> to/from JSON
 * Used by BridgeChainConfig to store contract addresses and metadata
 *
 * Automatically converts Java objects to JSON strings in the database
 * and back to Java objects when loading from database
 *
 * @author Claude Code - Priority 3 Implementation
 * @version 1.0.0
 */
@Converter
public class JsonStringMapConverter implements AttributeConverter<Map<String, String>, String> {

    private static final Logger logger = LoggerFactory.getLogger(JsonStringMapConverter.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Convert Map to JSON string for database storage
     *
     * @param attribute Map to convert (can be null)
     * @return JSON string or null if attribute is null
     */
    @Override
    public String convertToDatabaseColumn(Map<String, String> attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return null;
        }

        try {
            return objectMapper.writeValueAsString(attribute);
        } catch (Exception e) {
            logger.error("Error converting map to JSON string", e);
            return null;
        }
    }

    /**
     * Convert JSON string from database to Map
     *
     * @param dbData JSON string from database (can be null)
     * @return Map or empty Map if dbData is null/invalid
     */
    @Override
    public Map<String, String> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty()) {
            return new HashMap<>();
        }

        try {
            return objectMapper.readValue(dbData, Map.class);
        } catch (Exception e) {
            logger.error("Error converting JSON string to map: {}", dbData, e);
            return new HashMap<>();
        }
    }
}
