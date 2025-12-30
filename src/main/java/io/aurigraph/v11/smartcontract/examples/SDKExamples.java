package io.aurigraph.v11.smartcontract.examples;

import io.aurigraph.v11.smartcontract.SmartContract;
import io.aurigraph.v11.smartcontract.ContractExecution;
import io.aurigraph.v11.smartcontract.ContractMetadata;
import io.aurigraph.v11.smartcontract.sdk.AurigraphSDKClient;

import java.util.Map;
import java.util.List;

/**
 * Aurigraph Smart Contract SDK Examples
 *
 * Demonstrates how to use the Aurigraph Smart Contract SDK to deploy
 * and interact with smart contracts on the Aurigraph DLT platform.
 *
 * @version 11.2.1
 * @since 2025-10-12
 */
public class SDKExamples {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(SDKExamples.class);

    /**
     * Example 1: Deploy a Token Contract
     */
    public static void deployTokenContractExample() {
        // Initialize SDK client
        AurigraphSDKClient client = new AurigraphSDKClient("https://dlt.aurigraph.io/api/v11");

        // Create token contract
        SmartContract tokenContract = new SmartContract(
            "MyToken",
            TokenContract.SOURCE_CODE,
            SmartContract.ContractLanguage.JAVA,
            "owner123"
        );
        tokenContract.setVersion("1.0.0");
        tokenContract.setAbi(TokenContract.ABI);

        // Set metadata
        ContractMetadata metadata = new ContractMetadata();
        metadata.setDescription("ERC-20 style token on Aurigraph DLT");
        metadata.setAuthor("Aurigraph Development Team");
        metadata.setLicense("MIT");
        metadata.setTags(new String[]{"token", "erc20", "fungible"});
        metadata.setGasLimit(1000000L);
        tokenContract.setMetadata(metadata);

        // Deploy contract
        SmartContract deployed = client.deployContract(tokenContract).join();

        log.info("✅ Token contract deployed!");
        log.info("   Contract ID: " + deployed.getContractId());
        log.info("   Status: " + deployed.getStatus());
        log.info("   Deployed At: " + deployed.getDeployedAt());
    }

    /**
     * Example 2: Execute Token Transfer
     */
    public static void executeTokenTransferExample(String contractId) {
        AurigraphSDKClient client = new AurigraphSDKClient("https://dlt.aurigraph.io/api/v11");

        // Prepare transfer parameters
        Map<String, Object> params = Map.of(
            "to", "recipient456",
            "amount", 100
        );

        // Execute transfer method
        ContractExecution execution = client.executeContract(
            contractId,
            "transfer",
            params,
            "caller123"
        ).join();

        log.info("✅ Transfer executed!");
        log.info("   Execution ID: " + execution.getExecutionId());
        log.info("   Status: " + execution.getStatus());
        log.info("   Gas Used: " + execution.getGasUsed());
        log.info("   Result: " + execution.getResult());
    }

    /**
     * Example 3: Query Contract Balance
     */
    public static void queryBalanceExample(String contractId) {
        AurigraphSDKClient client = new AurigraphSDKClient("https://dlt.aurigraph.io/api/v11");

        // Query balance
        Map<String, Object> params = Map.of("account", "user123");

        ContractExecution execution = client.executeContract(
            contractId,
            "balanceOf",
            params,
            "anonymous"
        ).join();

        log.info("✅ Balance query completed!");
        log.info("   Account: user123");
        log.info("   Balance: " + execution.getResult());
    }

    /**
     * Example 4: List All Contracts
     */
    public static void listContractsExample() {
        AurigraphSDKClient client = new AurigraphSDKClient("https://dlt.aurigraph.io/api/v11");

        // List all contracts
        List<SmartContract> contracts = client.listContracts().join();

        log.info("✅ Contracts retrieved!");
        log.info("   Total Contracts: " + contracts.size());

        contracts.forEach(contract -> {
            log.info("\n   Contract: " + contract.getName());
            log.info("   ID: " + contract.getContractId());
            log.info("   Owner: " + contract.getOwner());
            log.info("   Status: " + contract.getStatus());
            log.info("   Language: " + contract.getLanguage());
        });
    }

    /**
     * Example 5: Get Contract Execution History
     */
    public static void getExecutionHistoryExample(String contractId) {
        AurigraphSDKClient client = new AurigraphSDKClient("https://dlt.aurigraph.io/api/v11");

        // Get execution history
        List<ContractExecution> history = client.getExecutionHistory(contractId).join();

        log.info("✅ Execution history retrieved!");
        log.info("   Total Executions: " + history.size());

        history.forEach(execution -> {
            log.info("\n   Execution: " + execution.getExecutionId());
            log.info("   Method: " + execution.getMethod());
            log.info("   Caller: " + execution.getCaller());
            log.info("   Status: " + execution.getStatus());
            log.info("   Gas Used: " + execution.getGasUsed());
            log.info("   Time: " + execution.getExecutionTimeMs() + "ms");
        });
    }

    /**
     * Example 6: Pause and Resume Contract
     */
    public static void pauseResumeContractExample(String contractId) {
        AurigraphSDKClient client = new AurigraphSDKClient("https://dlt.aurigraph.io/api/v11");

        // Pause contract
        SmartContract paused = client.pauseContract(contractId).join();
        log.info("✅ Contract paused!");
        log.info("   Status: " + paused.getStatus());

        // Resume contract
        SmartContract resumed = client.resumeContract(contractId).join();
        log.info("✅ Contract resumed!");
        log.info("   Status: " + resumed.getStatus());
    }

    /**
     * Example 7: Complete Token Workflow
     */
    public static void completeTokenWorkflowExample() {
        AurigraphSDKClient client = new AurigraphSDKClient("https://dlt.aurigraph.io/api/v11");

        log.info("=== Complete Token Workflow Example ===\n");

        // Step 1: Deploy token contract
        log.info("Step 1: Deploying token contract...");
        SmartContract contract = new SmartContract(
            "AurigraphToken",
            TokenContract.SOURCE_CODE,
            SmartContract.ContractLanguage.JAVA,
            "deployer123"
        );
        SmartContract deployed = client.deployContract(contract).join();
        log.info("✅ Deployed: " + deployed.getContractId() + "\n");

        // Step 2: Mint initial tokens
        log.info("Step 2: Minting initial tokens...");
        Map<String, Object> mintParams = Map.of("to", "user123", "amount", 1000);
        ContractExecution mintExec = client.executeContract(
            deployed.getContractId(),
            "mint",
            mintParams,
            "deployer123"
        ).join();
        log.info("✅ Minted 1000 tokens to user123\n");

        // Step 3: Transfer tokens
        log.info("Step 3: Transferring tokens...");
        Map<String, Object> transferParams = Map.of("to", "user456", "amount", 200);
        ContractExecution transferExec = client.executeContract(
            deployed.getContractId(),
            "transfer",
            transferParams,
            "user123"
        ).join();
        log.info("✅ Transferred 200 tokens to user456\n");

        // Step 4: Check balances
        log.info("Step 4: Checking balances...");
        Map<String, Object> balanceParams1 = Map.of("account", "user123");
        ContractExecution balance1 = client.executeContract(
            deployed.getContractId(),
            "balanceOf",
            balanceParams1,
            "anonymous"
        ).join();
        log.info("✅ user123 balance: " + balance1.getResult());

        Map<String, Object> balanceParams2 = Map.of("account", "user456");
        ContractExecution balance2 = client.executeContract(
            deployed.getContractId(),
            "balanceOf",
            balanceParams2,
            "anonymous"
        ).join();
        log.info("✅ user456 balance: " + balance2.getResult() + "\n");

        // Step 5: View execution history
        log.info("Step 5: Viewing execution history...");
        List<ContractExecution> history = client.getExecutionHistory(deployed.getContractId()).join();
        log.info("✅ Total executions: " + history.size());

        log.info("\n=== Workflow Complete! ===");
    }

    /**
     * Main method - Run all examples
     */
    public static void main(String[] args) {
        log.info("╔════════════════════════════════════════════════╗");
        log.info("║  Aurigraph Smart Contract SDK Examples        ║");
        log.info("║  Version 11.2.1                                ║");
        log.info("╚════════════════════════════════════════════════╝\n");

        try {
            // Run complete workflow example
            completeTokenWorkflowExample();

        } catch (Exception e) {
            log.error("❌ Error running examples: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
