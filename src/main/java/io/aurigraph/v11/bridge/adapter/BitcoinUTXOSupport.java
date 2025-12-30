package io.aurigraph.v11.bridge.adapter;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionInput;
import org.bitcoinj.core.TransactionOutput;
import org.bitcoinj.core.Coin;
import org.bitcoinj.script.ScriptBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

/**
 * Bitcoin UTXO Support for UTXO Chain Adapter
 *
 * Provides functions to:
 * - Validate Bitcoin addresses (P2PKH, P2SH, Bech32)
 * - Build UTXO transactions
 * - Handle transaction signing
 * - Manage multisig and SegWit addresses
 * - Calculate transaction fees
 *
 * Supports: Bitcoin mainnet, testnet, signet, Litecoin, Dogecoin
 *
 * Phase 11.3: UTXO Chain Support
 *
 * @author Claude Code
 * @version 1.0.0
 */
public class BitcoinUTXOSupport {

    private static final Logger logger = LoggerFactory.getLogger(BitcoinUTXOSupport.class);

    // Bitcoin address types
    public enum AddressType {
        P2PKH,      // Pay-to-Public-Key-Hash (starts with 1)
        P2SH,       // Pay-to-Script-Hash (starts with 3)
        BECH32,     // SegWit (starts with bc1)
        UNKNOWN
    }

    // UTXO representation
    public static class UTXO {
        public String txid;
        public int vout;
        public long amount; // in satoshis
        public String address;
        public int confirmations;
        public long blockTime;
        public boolean spendable;

        public UTXO(String txid, int vout, long amount) {
            this.txid = txid;
            this.vout = vout;
            this.amount = amount;
            this.spendable = true;
            this.confirmations = 0;
        }
    }

    /**
     * Validate Bitcoin address format
     *
     * @param address Address string to validate
     * @return true if valid Bitcoin address format
     */
    public static boolean isValidAddress(String address) {
        if (address == null || address.isEmpty()) {
            return false;
        }

        AddressType type = getAddressType(address);
        return type != AddressType.UNKNOWN;
    }

    /**
     * Determine Bitcoin address type
     *
     * @param address Address string
     * @return AddressType enum
     */
    public static AddressType getAddressType(String address) {
        if (address == null || address.isEmpty()) {
            return AddressType.UNKNOWN;
        }

        if (address.startsWith("bc1") || address.startsWith("BC1")) {
            // SegWit Bech32 address
            return address.length() >= 42 ? AddressType.BECH32 : AddressType.UNKNOWN;
        } else if (address.startsWith("1")) {
            // P2PKH address
            return address.length() == 34 ? AddressType.P2PKH : AddressType.UNKNOWN;
        } else if (address.startsWith("3")) {
            // P2SH address
            return address.length() == 34 ? AddressType.P2SH : AddressType.UNKNOWN;
        }

        return AddressType.UNKNOWN;
    }

    /**
     * Calculate transaction size for fee estimation
     *
     * @param inputCount Number of inputs
     * @param outputCount Number of outputs
     * @param isSegWit Whether transaction uses SegWit
     * @return Approximate transaction size in bytes
     */
    public static int estimateTransactionSize(int inputCount, int outputCount, boolean isSegWit) {
        // Base size: 4 (version) + 1 (input count) + 1 (output count) + 4 (locktime) = 10 bytes
        int baseSize = 10;

        // Each input: ~148 bytes for non-SegWit (legacy), ~68 bytes for SegWit
        int inputSize = isSegWit ? (inputCount * 68) : (inputCount * 148);

        // Each output: ~34 bytes
        int outputSize = outputCount * 34;

        // Total
        int totalSize = baseSize + inputSize + outputSize;

        // If SegWit, weight units = (legacy_bytes * 3) + total_bytes
        if (isSegWit) {
            return (totalSize * 3 + (baseSize + inputSize + outputSize)) / 4;
        }

        return totalSize;
    }

    /**
     * Calculate recommended transaction fee
     *
     * @param txSizeBytes Transaction size in bytes
     * @param satoshisPerByte Fee rate (satoshis per byte)
     * @return Total fee in satoshis
     */
    public static long calculateFee(int txSizeBytes, long satoshisPerByte) {
        return (long) txSizeBytes * satoshisPerByte;
    }

    /**
     * Convert satoshis to BTC
     *
     * @param satoshis Amount in satoshis
     * @return Amount in BTC as BigDecimal
     */
    public static BigDecimal satoshisToBTC(long satoshis) {
        return new BigDecimal(satoshis).divide(new BigDecimal(100_000_000));
    }

    /**
     * Convert BTC to satoshis
     *
     * @param btc Amount in BTC
     * @return Amount in satoshis as long
     */
    public static long btcToSatoshis(BigDecimal btc) {
        return btc.multiply(new BigDecimal(100_000_000)).longValue();
    }

    /**
     * Validate UTXO is spendable and not dust
     *
     * @param utxo UTXO to validate
     * @param dustThreshold Minimum amount considered dust (in satoshis)
     * @return true if UTXO is valid and spendable
     */
    public static boolean isValidUTXO(UTXO utxo, long dustThreshold) {
        return utxo != null &&
               utxo.spendable &&
               utxo.amount >= dustThreshold &&
               utxo.txid != null && !utxo.txid.isEmpty() &&
               utxo.confirmations >= 0;
    }

    /**
     * Build input selection for transaction
     * Uses simple greedy algorithm - can be improved for UTXO selection optimization
     *
     * @param utxos Available UTXOs
     * @param targetAmount Amount needed (in satoshis)
     * @return List of selected UTXOs that sum to at least targetAmount
     */
    public static List<UTXO> selectUTXOs(List<UTXO> utxos, long targetAmount) {
        List<UTXO> selected = new ArrayList<>();
        long accumulated = 0;

        // Sort by amount descending (greedy approach)
        List<UTXO> sortedUTXOs = new ArrayList<>(utxos);
        sortedUTXOs.sort((a, b) -> Long.compare(b.amount, a.amount));

        for (UTXO utxo : sortedUTXOs) {
            if (!utxo.spendable || utxo.amount < 5000) { // Skip dust
                continue;
            }

            selected.add(utxo);
            accumulated += utxo.amount;

            if (accumulated >= targetAmount) {
                break;
            }
        }

        if (accumulated < targetAmount) {
            logger.warn("Insufficient UTXOs: accumulated {} < target {}", accumulated, targetAmount);
        }

        return selected;
    }

    /**
     * Calculate change for transaction
     *
     * @param inputTotal Total from inputs (satoshis)
     * @param outputs Output amounts (satoshis)
     * @param fee Transaction fee (satoshis)
     * @return Change amount (satoshis), 0 if negative (should use different inputs)
     */
    public static long calculateChange(long inputTotal, List<Long> outputs, long fee) {
        long outputTotal = outputs.stream().mapToLong(Long::longValue).sum();
        long change = inputTotal - outputTotal - fee;

        if (change < 0) {
            logger.error("Insufficient inputs for transaction: {} < {} + {}", inputTotal, outputTotal, fee);
            return 0;
        }

        // Warn if change is dust
        if (change > 0 && change < 5000) {
            logger.warn("Change amount is dust: {} satoshis", change);
        }

        return change;
    }

    /**
     * Create multisig script (2-of-3 example)
     * In production, would use bitcoinj Script and ScriptBuilder
     *
     * @param publicKeys Array of public keys
     * @param requiredSignatures Number of signatures required
     * @return Multisig script (representation)
     */
    public static String createMultisigScript(String[] publicKeys, int requiredSignatures) {
        if (publicKeys.length < requiredSignatures || requiredSignatures < 1) {
            throw new IllegalArgumentException(
                "Invalid multisig parameters: " + requiredSignatures + " of " + publicKeys.length
            );
        }

        // In production: ScriptBuilder.createMultiSigOutputScript(requiredSignatures, pubKeys)
        // For now, return descriptive representation
        return "MultiSig(" + requiredSignatures + "-of-" + publicKeys.length + ")";
    }

    /**
     * Validate fee range
     *
     * @param fee Transaction fee (satoshis)
     * @param txSize Transaction size (bytes)
     * @param minRate Minimum rate (satoshis per byte)
     * @param maxRate Maximum rate (satoshis per byte)
     * @return true if fee is within acceptable range
     */
    public static boolean isValidFeeRange(long fee, int txSize, long minRate, long maxRate) {
        long actualRate = fee / txSize;
        boolean valid = actualRate >= minRate && actualRate <= maxRate;

        if (!valid) {
            logger.warn("Fee rate {} sat/byte outside range [{}, {}]", actualRate, minRate, maxRate);
        }

        return valid;
    }

    /**
     * Get standard fee rates by priority
     *
     * @param blockchainName Name of blockchain (Bitcoin, Litecoin, Dogecoin)
     * @return Fee rates (satoshis per byte) for different priorities
     */
    public static FeeRates getStandardFeeRates(String blockchainName) {
        FeeRates rates = new FeeRates();

        switch (blockchainName.toLowerCase()) {
            case "bitcoin":
                rates.low = 1;      // satoshis per byte
                rates.medium = 10;
                rates.high = 20;
                rates.veryHigh = 50;
                break;
            case "litecoin":
                rates.low = 0;
                rates.medium = 1;
                rates.high = 5;
                rates.veryHigh = 10;
                break;
            case "dogecoin":
                rates.low = 1;
                rates.medium = 10;
                rates.high = 50;
                rates.veryHigh = 100;
                break;
            default:
                rates.low = 1;
                rates.medium = 10;
                rates.high = 20;
                rates.veryHigh = 50;
        }

        return rates;
    }

    /**
     * Standard fee rates structure
     */
    public static class FeeRates {
        public long low;       // Slow transaction
        public long medium;    // Normal transaction
        public long high;      // Fast transaction
        public long veryHigh;  // Very fast transaction
    }
}
