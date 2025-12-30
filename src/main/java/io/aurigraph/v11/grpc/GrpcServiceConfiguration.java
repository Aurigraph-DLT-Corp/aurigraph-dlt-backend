package io.aurigraph.v11.grpc;

import io.quarkus.logging.Log;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

/**
 * gRPC Service Configuration for Aurigraph V11
 *
 * IMPORTANT: This class is now a placeholder for logging only.
 * Quarkus gRPC extension automatically discovers and registers all @GrpcService beans.
 * Services are auto-configured via application.properties (quarkus.grpc.server.port=9004).
 *
 * Architecture:
 * - HTTP/2 based communication for multiplexing
 * - Protocol Buffer serialization for type-safety
 * - Streaming support for real-time updates
 * - Zero-copy transmission for high performance
 *
 * Services auto-registered by Quarkus:
 * 1. TransactionService - Transaction processing and mempool management (@GrpcService)
 * 2. ConsensusService - HyperRAFT++ consensus and log replication (@GrpcService)
 * 3. NetworkService - Peer communication and message routing (@GrpcService)
 * 4. BlockchainService - Block creation, validation, streaming (standard gRPC)
 * 5. ContractService - Smart contract deployment and execution (TODO)
 * 6. TraceabilityService - Contract-asset link traceability (TODO)
 * 7. CryptoService - Quantum-resistant cryptographic operations (TODO)
 * 8. StorageService - State storage and key-value operations (TODO)
 */
@ApplicationScoped
public class GrpcServiceConfiguration {

    private static final int GRPC_PORT = 9004;

    /**
     * Log gRPC service registration on startup
     * Quarkus handles actual server lifecycle automatically
     */
    void onStart(@Observes StartupEvent event) {
        Log.infof("Quarkus gRPC server will start on port %d", GRPC_PORT);
        Log.info("gRPC services will be auto-registered by Quarkus:");
        Log.info("   - TransactionService (@GrpcService with Mutiny reactive streams)");
        Log.info("   - ConsensusService (@GrpcService with Mutiny reactive streams)");
        Log.info("   - NetworkService (@GrpcService with Mutiny reactive streams)");
        Log.info("   - BlockchainService (standard gRPC with StreamObserver)");
        Log.info("   - Additional services will be discovered automatically if present");
    }

    /**
     * Get the configured gRPC port
     */
    public int getGrpcPort() {
        return GRPC_PORT;
    }
}
