package test.bankcardmanagement.service;


import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import test.bankcardmanagement.dto.request.TransferRequest;
import test.bankcardmanagement.entity.BankCard;
import test.bankcardmanagement.entity.Transaction;
import test.bankcardmanagement.exception.InsufficientFundsException;
import test.bankcardmanagement.exception.OperationNotAllowedException;
import test.bankcardmanagement.repository.BankCardRepository;
import test.bankcardmanagement.repository.TransactionRepository;

import java.time.LocalDate;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransferService {

    private final BankCardRepository cardRepository;
    private final TransactionRepository transactionRepository;
    private final EncryptionService encryptionService;

    @Transactional
    public Transaction transferBetweenOwnCards(TransferRequest request) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();

        String fromHash = encryptionService.hash(request.getFromCardNumber());
        String toHash = encryptionService.hash(request.getToCardNumber());

        BankCard fromCard = cardRepository.findByCardNumberHash(fromHash)
                .orElseThrow(() -> new RuntimeException("From card not found"));
        BankCard toCard = cardRepository.findByCardNumberHash(toHash)
                .orElseThrow(() -> new RuntimeException("To card not found"));

        if (!fromCard.getUser().getUsername().equals(username) ||
                !toCard.getUser().getUsername().equals(username)) {
            throw new OperationNotAllowedException("You can only transfer between your own cards");
        }

        checkCardStatus(fromCard);
        checkCardStatus(toCard);

        if (fromCard.getBalance().compareTo(request.getAmount()) < 0) {
            throw new InsufficientFundsException("Insufficient funds");
        }

        // Выполнить перевод
        fromCard.setBalance(fromCard.getBalance().subtract(request.getAmount()));
        toCard.setBalance(toCard.getBalance().add(request.getAmount()));

        cardRepository.save(fromCard);
        cardRepository.save(toCard);

        // Создать запись о транзакции
        Transaction transaction = Transaction.builder()
                .transactionId(UUID.randomUUID().toString())
                .fromCard(fromCard)
                .toCard(toCard)
                .amount(request.getAmount())
                .description(request.getDescription())
                .status(Transaction.TransactionStatus.COMPLETED)
                .build();

        return transactionRepository.save(transaction);
    }

    private void checkCardStatus(BankCard card) {
        if (card.getStatus() != BankCard.CardStatus.ACTIVE) {
            throw new OperationNotAllowedException(
                    String.format("Card is %s. Only active cards can perform transfers",
                            card.getStatus().toString().toLowerCase())
            );
        }

        if (card.getExpirationDate().isBefore(LocalDate.now())) {
            throw new OperationNotAllowedException("Card is expired");
        }
    }
}