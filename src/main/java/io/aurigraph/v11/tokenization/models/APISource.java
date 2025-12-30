package io.aurigraph.v11.tokenization.models;

import java.util.Map;

/**
 * API Source Model
 *
 * Represents an external API source for tokenization
 *
 * @version 11.3.0
 * @author Backend Development Agent (BDA)
 */
public class APISource {

    public String id;
    public String name;
    public String url;
    public String method;
    public Map<String, String> headers;
    public String channel;
    public String status;  // "active", "paused", "error"
    public int pollInterval;  // seconds
    public String lastFetch;  // ISO timestamp
    public int totalTokenized;
    public int errorCount;

    public APISource() {
        // Default constructor for Jackson/JSON deserialization
    }

    public APISource(String id, String name, String url, String method,
                    Map<String, String> headers, String channel,
                    String status, int pollInterval) {
        this.id = id;
        this.name = name;
        this.url = url;
        this.method = method;
        this.headers = headers;
        this.channel = channel;
        this.status = status;
        this.pollInterval = pollInterval;
        this.totalTokenized = 0;
        this.errorCount = 0;
    }
}
