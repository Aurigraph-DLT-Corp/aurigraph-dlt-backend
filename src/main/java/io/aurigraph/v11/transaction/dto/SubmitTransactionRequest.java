package io.aurigraph.v11.transaction.dto;

import io.aurigraph.v11.transaction.models.Transaction;
import java.math.BigDecimal;

public class SubmitTransactionRequest {
    public String userId;
    public String fromAddress;
    public String toAddress;
    public BigDecimal amount;
    public Transaction.TransactionType transactionType;
    public Long gasLimit;
    public BigDecimal gasPrice;
    public String data;
}
