package io.aurigraph.v11.ai;

import jakarta.enterprise.context.ApplicationScoped;
import io.smallrye.mutiny.Uni;

/**
 * AI Integration Service stub
 * Full implementation pending - currently disabled in tests
 */
@ApplicationScoped
public class AIIntegrationService {

    public Uni<Boolean> initialize() {
        return Uni.createFrom().item(false);
    }

    public Uni<Boolean> isEnabled() {
        return Uni.createFrom().item(false);
    }
}
