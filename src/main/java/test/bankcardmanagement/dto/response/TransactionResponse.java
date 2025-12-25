package test.bankcardmanagement.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import test.bankcardmanagement.entity.Transaction;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionResponse {

    private String transactionId;
    private String fromCardMasked;
    private String toCardMasked;
    private BigDecimal amount;
    private LocalDateTime timestamp;
    private Transaction.TransactionStatus status;
    private String description;

    public static TransactionResponse fromEntity(Transaction transaction) {
        return TransactionResponse.builder()
                .transactionId(transaction.getTransactionId())
                .fromCardMasked("**** **** **** " +
                        transaction.getFromCard().getLastFourDigits())
                .toCardMasked("**** **** **** " +
                        transaction.getToCard().getLastFourDigits())
                .amount(transaction.getAmount())
                .timestamp(transaction.getTimestamp())
                .status(transaction.getStatus())
                .description(transaction.getDescription())
                .build();
    }
}