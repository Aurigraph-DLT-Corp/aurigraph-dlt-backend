package io.aurigraph.v11.smartcontract.examples;

/**
 * Example Token Smart Contract
 *
 * Simple ERC-20 style token contract for Aurigraph DLT.
 * This is an example contract code that can be deployed using the SDK.
 *
 * @version 11.2.1
 * @since 2025-10-12
 */
public class TokenContract {

    /**
     * Token contract source code (simplified Java-style)
     */
    public static final String SOURCE_CODE = """
        // Simple Token Contract for Aurigraph DLT

        contract Token {
            // State variables
            String public name;
            String public symbol;
            int public totalSupply;
            Map<String, Integer> public balances;

            // Constructor
            constructor(String _name, String _symbol, int _initialSupply) {
                name = _name;
                symbol = _symbol;
                totalSupply = _initialSupply;
                balances.put(msg.sender, _initialSupply);
            }

            // Transfer tokens
            function transfer(String to, int amount) public returns (boolean) {
                require(balances.get(msg.sender) >= amount, "Insufficient balance");

                balances.put(msg.sender, balances.get(msg.sender) - amount);
                balances.put(to, balances.getOrDefault(to, 0) + amount);

                emit Transfer(msg.sender, to, amount);
                return true;
            }

            // Get balance
            function balanceOf(String account) public view returns (int) {
                return balances.getOrDefault(account, 0);
            }

            // Mint new tokens (only owner)
            function mint(String to, int amount) public onlyOwner {
                totalSupply += amount;
                balances.put(to, balances.getOrDefault(to, 0) + amount);

                emit Mint(to, amount);
            }

            // Burn tokens
            function burn(int amount) public {
                require(balances.get(msg.sender) >= amount, "Insufficient balance");

                balances.put(msg.sender, balances.get(msg.sender) - amount);
                totalSupply -= amount;

                emit Burn(msg.sender, amount);
            }

            // Events
            event Transfer(String from, String to, int amount);
            event Mint(String to, int amount);
            event Burn(String from, int amount);
        }
        """;

    /**
     * Token contract ABI (Application Binary Interface)
     */
    public static final String ABI = """
        {
          "name": "Token",
          "methods": [
            {
              "name": "constructor",
              "inputs": [
                {"name": "_name", "type": "string"},
                {"name": "_symbol", "type": "string"},
                {"name": "_initialSupply", "type": "int"}
              ]
            },
            {
              "name": "transfer",
              "inputs": [
                {"name": "to", "type": "string"},
                {"name": "amount", "type": "int"}
              ],
              "outputs": [{"type": "boolean"}]
            },
            {
              "name": "balanceOf",
              "inputs": [{"name": "account", "type": "string"}],
              "outputs": [{"type": "int"}],
              "view": true
            },
            {
              "name": "mint",
              "inputs": [
                {"name": "to", "type": "string"},
                {"name": "amount", "type": "int"}
              ],
              "modifiers": ["onlyOwner"]
            },
            {
              "name": "burn",
              "inputs": [{"name": "amount", "type": "int"}]
            }
          ],
          "events": [
            {
              "name": "Transfer",
              "inputs": [
                {"name": "from", "type": "string", "indexed": true},
                {"name": "to", "type": "string", "indexed": true},
                {"name": "amount", "type": "int"}
              ]
            },
            {
              "name": "Mint",
              "inputs": [
                {"name": "to", "type": "string", "indexed": true},
                {"name": "amount", "type": "int"}
              ]
            },
            {
              "name": "Burn",
              "inputs": [
                {"name": "from", "type": "string", "indexed": true},
                {"name": "amount", "type": "int"}
              ]
            }
          ]
        }
        """;
}
