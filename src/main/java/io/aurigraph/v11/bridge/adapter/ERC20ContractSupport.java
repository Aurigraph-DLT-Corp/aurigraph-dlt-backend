package io.aurigraph.v11.bridge.adapter;

import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.EthCall;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Uint256;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * ERC20 Contract Support for Web3j Chain Adapter
 *
 * Provides functions to:
 * - Query ERC20 token balances
 * - Get token metadata (decimals, symbol, name)
 * - Handle standard ERC20 contract calls
 * - Support for token transfer events
 *
 * Phase 11.2: Web3j Enhancement
 *
 * @author Claude Code
 * @version 1.0.0
 */
public class ERC20ContractSupport {

    private static final Logger logger = LoggerFactory.getLogger(ERC20ContractSupport.class);

    /**
     * Get ERC20 token balance for an address
     * Calls balanceOf(address) function on ERC20 contract
     *
     * @param web3j Web3j instance
     * @param contractAddress Token contract address
     * @param walletAddress Address to query balance for
     * @return Token balance as BigInteger (in smallest units)
     * @throws Exception if call fails
     */
    public static BigInteger getERC20Balance(Web3j web3j, String contractAddress, String walletAddress) throws Exception {
        // Create function: balanceOf(address)
        Function function = new Function(
            "balanceOf",
            Arrays.asList(new Address(walletAddress)),
            Arrays.asList(new TypeReference<Uint256>() {})
        );

        String encodedFunction = FunctionEncoder.encode(function);

        // Execute eth_call
        EthCall response = web3j.ethCall(
            org.web3j.protocol.core.methods.request.Transaction.createEthCallTransaction(
                null,
                contractAddress,
                encodedFunction
            ),
            org.web3j.protocol.core.DefaultBlockParameterName.LATEST
        ).send();

        if (response.hasError()) {
            logger.error("Error querying ERC20 balance: {}", response.getError().getMessage());
            return BigInteger.ZERO;
        }

        // Decode the response
        List<Type> types = FunctionReturnDecoder.decode(response.getValue(), function.getOutputParameters());
        if (types.isEmpty()) {
            return BigInteger.ZERO;
        }

        return (BigInteger) types.get(0).getValue();
    }

    /**
     * Get ERC20 token decimals
     * Calls decimals() function on ERC20 contract
     *
     * @param web3j Web3j instance
     * @param contractAddress Token contract address
     * @return Number of decimals (typically 18 for most tokens)
     * @throws Exception if call fails
     */
    public static int getERC20Decimals(Web3j web3j, String contractAddress) throws Exception {
        Function function = new Function(
            "decimals",
            Collections.emptyList(),
            Arrays.asList(new TypeReference<org.web3j.abi.datatypes.generated.Uint8>() {})
        );

        String encodedFunction = FunctionEncoder.encode(function);

        EthCall response = web3j.ethCall(
            org.web3j.protocol.core.methods.request.Transaction.createEthCallTransaction(
                null,
                contractAddress,
                encodedFunction
            ),
            org.web3j.protocol.core.DefaultBlockParameterName.LATEST
        ).send();

        if (response.hasError()) {
            logger.warn("Could not get decimals for contract {}: {}", contractAddress, response.getError().getMessage());
            return 18; // Default to 18
        }

        List<Type> types = FunctionReturnDecoder.decode(response.getValue(), function.getOutputParameters());
        if (types.isEmpty()) {
            return 18;
        }

        return ((org.web3j.abi.datatypes.generated.Uint8) types.get(0)).getValue().intValue();
    }

    /**
     * Get ERC20 token symbol
     * Calls symbol() function on ERC20 contract
     *
     * @param web3j Web3j instance
     * @param contractAddress Token contract address
     * @return Token symbol string
     * @throws Exception if call fails
     */
    public static String getERC20Symbol(Web3j web3j, String contractAddress) throws Exception {
        Function function = new Function(
            "symbol",
            Collections.emptyList(),
            Arrays.asList(new TypeReference<org.web3j.abi.datatypes.Utf8String>() {})
        );

        String encodedFunction = FunctionEncoder.encode(function);

        EthCall response = web3j.ethCall(
            org.web3j.protocol.core.methods.request.Transaction.createEthCallTransaction(
                null,
                contractAddress,
                encodedFunction
            ),
            org.web3j.protocol.core.DefaultBlockParameterName.LATEST
        ).send();

        if (response.hasError()) {
            logger.warn("Could not get symbol for contract {}: {}", contractAddress, response.getError().getMessage());
            return "UNKNOWN";
        }

        List<Type> types = FunctionReturnDecoder.decode(response.getValue(), function.getOutputParameters());
        if (types.isEmpty()) {
            return "UNKNOWN";
        }

        return ((org.web3j.abi.datatypes.Utf8String) types.get(0)).getValue();
    }

    /**
     * Get ERC20 token total supply
     * Calls totalSupply() function on ERC20 contract
     *
     * @param web3j Web3j instance
     * @param contractAddress Token contract address
     * @return Total supply as BigInteger
     * @throws Exception if call fails
     */
    public static BigInteger getERC20TotalSupply(Web3j web3j, String contractAddress) throws Exception {
        Function function = new Function(
            "totalSupply",
            Collections.emptyList(),
            Arrays.asList(new TypeReference<Uint256>() {})
        );

        String encodedFunction = FunctionEncoder.encode(function);

        EthCall response = web3j.ethCall(
            org.web3j.protocol.core.methods.request.Transaction.createEthCallTransaction(
                null,
                contractAddress,
                encodedFunction
            ),
            org.web3j.protocol.core.DefaultBlockParameterName.LATEST
        ).send();

        if (response.hasError()) {
            logger.warn("Could not get total supply for contract {}: {}", contractAddress, response.getError().getMessage());
            return BigInteger.ZERO;
        }

        List<Type> types = FunctionReturnDecoder.decode(response.getValue(), function.getOutputParameters());
        if (types.isEmpty()) {
            return BigInteger.ZERO;
        }

        return (BigInteger) types.get(0).getValue();
    }

    /**
     * Convert token amount from raw units to decimal-adjusted amount
     *
     * @param rawAmount Amount in smallest units (e.g., wei for 18-decimal token)
     * @param decimals Number of decimal places
     * @return Decimal-adjusted amount
     */
    public static BigDecimal toDecimalAmount(BigInteger rawAmount, int decimals) {
        if (decimals < 0) {
            decimals = 18;
        }
        BigDecimal divisor = BigDecimal.TEN.pow(decimals);
        return new BigDecimal(rawAmount).divide(divisor);
    }

    /**
     * Convert token amount from decimal representation to raw units
     *
     * @param decimalAmount Decimal-adjusted amount
     * @param decimals Number of decimal places
     * @return Raw amount in smallest units
     */
    public static BigInteger fromDecimalAmount(BigDecimal decimalAmount, int decimals) {
        if (decimals < 0) {
            decimals = 18;
        }
        BigDecimal multiplier = BigDecimal.TEN.pow(decimals);
        return decimalAmount.multiply(multiplier).toBigInteger();
    }
}
