package test.bankcardmanagement.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import test.bankcardmanagement.entity.BankCard;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardResponse {

    private Long id;
    private String maskedCardNumber;
    private String cardHolderName;
    private LocalDate expirationDate;
    private BankCard.CardStatus status;
    private BigDecimal balance;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long userId;

    public static CardResponse fromEntity(BankCard card) {
        return CardResponse.builder()
                .id(card.getId())
                .maskedCardNumber("**** **** **** " + card.getLastFourDigits())
                .cardHolderName(card.getCardHolderName())
                .expirationDate(card.getExpirationDate())
                .status(card.getStatus())
                .balance(card.getBalance())
                .createdAt(card.getCreatedAt())
                .updatedAt(card.getUpdatedAt())
                .userId(card.getUser().getId())
                .build();
    }
}