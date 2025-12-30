package io.aurigraph.v11.contracts.composite;

/**
 * Types of secondary tokens in composite packages
 */
public enum SecondaryTokenType {
    OWNER,        // Owner token (ERC-721)
    COLLATERAL,   // Collateral token (ERC-1155)
    MEDIA,        // Media token (ERC-1155)
    VERIFICATION, // Verification token (ERC-721)
    VALUATION,    // Valuation token (ERC-20)
    COMPLIANCE    // Compliance token (ERC-721)
}