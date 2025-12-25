package test.bankcardmanagement.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import test.bankcardmanagement.dto.request.CardCreateRequest;
import test.bankcardmanagement.dto.response.CardResponse;
import test.bankcardmanagement.entity.BankCard;
import test.bankcardmanagement.entity.User;
import test.bankcardmanagement.exception.CardNotFoundException;
import test.bankcardmanagement.exception.OperationNotAllowedException;
import test.bankcardmanagement.repository.BankCardRepository;
import test.bankcardmanagement.repository.UserRepository;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class CardService {

    private final BankCardRepository cardRepository;
    private final UserRepository userRepository;
    private final EncryptionService encryptionService;

    @Transactional
    public CardResponse createCard(CardCreateRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Проверка номера карты (Luhn алгоритм)
        if (!isValidCardNumber(request.getCardNumber())) {
            throw new IllegalArgumentException("Invalid card number");
        }

        String encryptedCardNumber = encryptionService.encrypt(request.getCardNumber());
        String cardHash = encryptionService.hash(request.getCardNumber());
        String lastFourDigits = request.getCardNumber().substring(12);

        // Проверка уникальности карты
        if (cardRepository.existsByCardNumberHash(cardHash)) {
            throw new RuntimeException("Card already exists");
        }

        BankCard card = BankCard.builder()
                .cardNumberEncrypted(encryptedCardNumber)
                .cardNumberHash(cardHash)
                .lastFourDigits(lastFourDigits)
                .cardHolderName(request.getCardHolderName())
                .expirationDate(request.getExpirationDate())
                .balance(request.getInitialBalance())
                .user(user)
                .build();

        card = cardRepository.save(card);
        return CardResponse.fromEntity(card);
    }

    @Transactional(readOnly = true)
    public CardResponse getCardById(Long id) {
        BankCard card = cardRepository.findById(id)
                .orElseThrow(() -> new CardNotFoundException("Card not found with id: " + id));

        checkCardAccessPermission(card);
        return CardResponse.fromEntity(card);
    }

    @Transactional(readOnly = true)
    public Page<CardResponse> getUserCards(Pageable pageable) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return cardRepository.findByUser(user, pageable)
                .map(CardResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public Page<CardResponse> getAllCards(Pageable pageable) {
        return cardRepository.findAll(pageable)
                .map(CardResponse::fromEntity);
    }

    @Transactional
    public CardResponse updateCardStatus(Long id, BankCard.CardStatus newStatus) {
        BankCard card = cardRepository.findById(id)
                .orElseThrow(() -> new CardNotFoundException("Card not found with id: " + id));

        checkCardAccessPermission(card);

        if (card.getExpirationDate().isBefore(LocalDate.now())) {
            throw new OperationNotAllowedException("Cannot update status of expired card");
        }

        card.setStatus(newStatus);
        card = cardRepository.save(card);
        return CardResponse.fromEntity(card);
    }

    @Transactional
    public void deleteCard(Long id) {
        BankCard card = cardRepository.findById(id)
                .orElseThrow(() -> new CardNotFoundException("Card not found with id: " + id));

        if (!isAdmin()) {
            throw new OperationNotAllowedException("Admin permission required");
        }

        cardRepository.delete(card);
    }

    private boolean isValidCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.length() != 16) {
            return false;
        }

        int sum = 0;
        boolean alternate = false;
        for (int i = cardNumber.length() - 1; i >= 0; i--) {
            int digit = Character.getNumericValue(cardNumber.charAt(i));
            if (alternate) {
                digit *= 2;
                if (digit > 9) {
                    digit = (digit % 10) + 1;
                }
            }
            sum += digit;
            alternate = !alternate;
        }
        return (sum % 10 == 0);
    }

    private void checkCardAccessPermission(BankCard card) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        boolean isAdmin = isAdmin();

        if (!isAdmin && !card.getUser().getUsername().equals(username)) {
            throw new OperationNotAllowedException("Access denied to this card");
        }
    }

    private boolean isAdmin() {
        return SecurityContextHolder.getContext().getAuthentication()
                .getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));
    }
}