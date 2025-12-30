package io.aurigraph.v11.api;

import org.eclipse.microprofile.openapi.annotations.OpenAPIDefinition;
import org.eclipse.microprofile.openapi.annotations.enums.SecuritySchemeIn;
import org.eclipse.microprofile.openapi.annotations.enums.SecuritySchemeType;
import org.eclipse.microprofile.openapi.annotations.info.Contact;
import org.eclipse.microprofile.openapi.annotations.info.Info;
import org.eclipse.microprofile.openapi.annotations.info.License;
import org.eclipse.microprofile.openapi.annotations.security.SecurityScheme;
import org.eclipse.microprofile.openapi.annotations.servers.Server;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import jakarta.ws.rs.core.Application;

/**
 * OpenAPI Configuration for Aurigraph V11 REST API
 *
 * Documents:
 * - Phase 1: 12 high-priority endpoints (AI, RWA, Bridge)
 * - Phase 2: 14 medium-priority endpoints (AI, Security, RWA, Bridge)
 * - Total: 26+ endpoints
 *
 * @version 3.7.3
 * @author Aurigraph V11 Team
 */
@OpenAPIDefinition(
    info = @Info(
        title = "Aurigraph V11 REST API",
        version = "12.0.0",
        description = "High-performance blockchain platform with quantum-resistant cryptography, " +
                      "AI-driven consensus optimization, real-world asset tokenization, and cross-chain interoperability. " +
                      "Supports 2M+ TPS with sub-100ms finality.",
        contact = @Contact(
            name = "Aurigraph Development Team",
            url = "https://aurigraph.io",
            email = "dev@aurigraph.io"
        ),
        license = @License(
            name = "Commercial License",
            url = "https://aurigraph.io/license"
        )
    ),
    servers = {
        @Server(
            url = "http://localhost:9003",
            description = "Local Development Server"
        ),
        @Server(
            url = "https://api.aurigraph.io",
            description = "Production API Server"
        ),
        @Server(
            url = "https://dev4.aurigraph.io",
            description = "Development Environment"
        )
    },
    tags = {
        @Tag(name = "AI Optimization", description = "AI-driven consensus optimization and model management"),
        @Tag(name = "Real-World Assets", description = "Real-world asset tokenization and portfolio management"),
        @Tag(name = "Cross-Chain Bridge", description = "Cross-chain asset transfers and bridge operations"),
        @Tag(name = "Security", description = "Security auditing, vulnerability scanning, and key management"),
        @Tag(name = "Health & Monitoring", description = "Service health checks and system monitoring"),
        @Tag(name = "Consensus & Transactions", description = "Consensus operations and transaction processing")
    }
)
@SecurityScheme(
    securitySchemeName = "bearerAuth",
    description = "JWT Bearer Token Authentication",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT",
    in = SecuritySchemeIn.HEADER
)
public class OpenAPIConfiguration extends Application {
    // Configuration for OpenAPI/Swagger documentation
}
