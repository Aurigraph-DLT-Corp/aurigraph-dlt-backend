package io.aurigraph.v11.bridge;

/**
 * Chain information
 */
public class ChainInfo {
    private final String chainId;
    private final String name;
    private final int networkId;
    private final String nativeCurrency;
    private final int decimals;
    private final boolean enabled;
    private final String bridgeContract;

    public ChainInfo(String chainId, String name, int networkId, String nativeCurrency,
                    int decimals, boolean enabled, String bridgeContract) {
        this.chainId = chainId;
        this.name = name;
        this.networkId = networkId;
        this.nativeCurrency = nativeCurrency;
        this.decimals = decimals;
        this.enabled = enabled;
        this.bridgeContract = bridgeContract;
    }

    // Getters
    public String getChainId() { return chainId; }
    public String getName() { return name; }
    public int getNetworkId() { return networkId; }
    public String getNativeCurrency() { return nativeCurrency; }
    public int getDecimals() { return decimals; }
    public boolean isEnabled() { return enabled; }
    public String getBridgeContract() { return bridgeContract; }
}
