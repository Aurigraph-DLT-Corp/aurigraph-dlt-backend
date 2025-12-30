package io.aurigraph.v11.bridge.factory;

import io.aurigraph.v11.bridge.adapter.Web3jChainAdapter;
import io.aurigraph.v11.bridge.adapter.SolanaChainAdapter;
import io.aurigraph.v11.bridge.adapter.CosmosChainAdapter;

/**
 * Classification of blockchains by consensus/VM type for adapter family pattern
 * Enables reusable adapter implementations across similar chains
 *
 * PHASE: 3 (Week 5-8) - Chain Adapter Implementation
 * 7 adapter families with full Mutiny reactive support
 *
 * @author Claude Code - Priority 3 Implementation
 * @version 2.0.0 - Phase 3 adapters with reactive support
 */
public enum ChainFamily {
    /**
     * Ethereum Virtual Machine - EVM-compatible chains
     * Examples: Ethereum, Polygon, BSC, Arbitrum, Optimism, Avalanche, Fantom, etc.
     * Total: 18+ chains
     * Adapter: Web3jChainAdapter (reactive with Mutiny)
     */
    EVM(
        "Ethereum Virtual Machine",
        "EVM-compatible chains using Web3.js/Web3j",
        Web3jChainAdapter.class
    ),

    /**
     * Solana Program Model - SPL token standard
     * Examples: Solana, Serum, Marinade, Magic Eden, Orca
     * Total: 5 chains
     * Adapter: SolanaChainAdapter (reactive with Mutiny)
     */
    SOLANA(
        "Solana Program Model",
        "Solana ecosystem with SPL tokens",
        SolanaChainAdapter.class
    ),

    /**
     * Cosmos SDK - IBC protocol for inter-blockchain communication
     * Examples: Cosmos Hub, Osmosis, Juno, Evmos, Injective, Kava, etc.
     * Total: 10 chains
     * Adapter: CosmosChainAdapter (reactive with Mutiny, IBC support)
     */
    COSMOS(
        "Cosmos SDK + IBC",
        "Cosmos ecosystem with IBC protocol",
        CosmosChainAdapter.class
    ),

    /**
     * Polkadot/Substrate - XCM for cross-chain messaging
     * Examples: Polkadot, Kusama, Moonbeam, Astar, Hydra DX, etc.
     * Total: 8 chains
     * Adapter: SubstrateChainAdapter (Week 3, Week 5-8)
     */
    SUBSTRATE(
        "Polkadot/Substrate",
        "Substrate-based chains with XCM",
        null  // Adapter: SubstrateChainAdapter - Week 5-8
    ),

    /**
     * Layer 2 Solutions - Optimistic and ZK rollups
     * Examples: Arbitrum, Optimism, zkSync, StarkNet, Scroll
     * Total: 5 chains
     * Adapter: Layer2ChainAdapter (Week 3, Week 5-8)
     */
    LAYER2(
        "Layer 2 Rollups",
        "L2 solutions (Optimistic & ZK rollups)",
        null  // Adapter: Layer2ChainAdapter - Week 5-8
    ),

    /**
     * UTXO Model - Bitcoin-style transaction model
     * Examples: Bitcoin, Litecoin, Dogecoin
     * Total: 3 chains
     * Adapter: UTXOChainAdapter (Week 4, Week 5-8)
     */
    UTXO(
        "Bitcoin UTXO Model",
        "UTXO-based chains like Bitcoin",
        null  // Adapter: UTXOChainAdapter - Week 5-8
    ),

    /**
     * Other Virtual Machines - Unique implementations
     * Examples: Tezos, Cardano, Near, Algorand, Hedera, Tron
     * Total: 6 chains
     * Adapter: GenericChainAdapter (Week 4, Week 5-8)
     */
    OTHER(
        "Other VMs",
        "Unique VM implementations (Tezos, Cardano, Near, etc.)",
        null  // Adapter: GenericChainAdapter - Week 5-8
    );

    private final String displayName;
    private final String description;
    private final Class<?> adapterClass;

    ChainFamily(String displayName, String description, Class<?> adapterClass) {
        this.displayName = displayName;
        this.description = description;
        this.adapterClass = adapterClass;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public Class<?> getAdapterClass() {
        return adapterClass;
    }

    /**
     * Get family by name (case-insensitive)
     */
    public static ChainFamily fromString(String name) {
        try {
            return ChainFamily.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            return OTHER; // Default to generic adapter
        }
    }
}
