package test.bankcardmanagement.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import test.bankcardmanagement.dto.request.TransferRequest;
import test.bankcardmanagement.entity.BankCard;
import test.bankcardmanagement.entity.Transaction;
import test.bankcardmanagement.entity.User;
import test.bankcardmanagement.exception.InsufficientFundsException;
import test.bankcardmanagement.exception.OperationNotAllowedException;
import test.bankcardmanagement.repository.BankCardRepository;
import test.bankcardmanagement.repository.TransactionRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransferServiceTest {

    @Mock
    private BankCardRepository cardRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private EncryptionService encryptionService;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private TransferService transferService;

    private User testUser;
    private BankCard fromCard;
    private BankCard toCard;
    private TransferRequest transferRequest;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .build();

        fromCard = BankCard.builder()
                .id(1L)
                .cardNumberEncrypted("encrypted_from")
                .cardNumberHash("hash_from")
                .lastFourDigits("1111")
                .cardHolderName("Test User")
                .expirationDate(LocalDate.now().plusYears(1))
                .status(BankCard.CardStatus.ACTIVE)
                .balance(BigDecimal.valueOf(1000))
                .user(testUser)
                .build();

        toCard = BankCard.builder()
                .id(2L)
                .cardNumberEncrypted("encrypted_to")
                .cardNumberHash("hash_to")
                .lastFourDigits("2222")
                .cardHolderName("Test User")
                .expirationDate(LocalDate.now().plusYears(1))
                .status(BankCard.CardStatus.ACTIVE)
                .balance(BigDecimal.valueOf(500))
                .user(testUser)
                .build();

        transferRequest = TransferRequest.builder()
                .fromCardNumber("4111111111111111")
                .toCardNumber("5555555555554444")
                .amount(BigDecimal.valueOf(100))
                .description("Test transfer")
                .build();

        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    void transferBetweenOwnCards_SuccessfulTransfer() {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("testuser");
        when(encryptionService.hash("4111111111111111")).thenReturn("hash_from");
        when(encryptionService.hash("5555555555554444")).thenReturn("hash_to");
        when(cardRepository.findByCardNumberHash("hash_from")).thenReturn(Optional.of(fromCard));
        when(cardRepository.findByCardNumberHash("hash_to")).thenReturn(Optional.of(toCard));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction transaction = invocation.getArgument(0);
            transaction.setId(1L);
            return transaction;
        });

        Transaction result = transferService.transferBetweenOwnCards(transferRequest);

        assertNotNull(result);
        assertEquals(Transaction.TransactionStatus.COMPLETED, result.getStatus());
        assertEquals(BigDecimal.valueOf(100), result.getAmount());
        assertEquals("Test transfer", result.getDescription());
        assertEquals(fromCard, result.getFromCard());
        assertEquals(toCard, result.getToCard());

        assertEquals(BigDecimal.valueOf(900), fromCard.getBalance());
        assertEquals(BigDecimal.valueOf(600), toCard.getBalance());

        verify(cardRepository).save(fromCard);
        verify(cardRepository).save(toCard);
        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    void transferBetweenOwnCards_FromCardNotFound() {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("testuser");
        when(encryptionService.hash("4111111111111111")).thenReturn("hash_from");
        when(cardRepository.findByCardNumberHash("hash_from")).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> transferService.transferBetweenOwnCards(transferRequest));

        assertEquals("From card not found", exception.getMessage());
        verify(cardRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void transferBetweenOwnCards_ToCardNotFound() {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("testuser");
        when(encryptionService.hash("4111111111111111")).thenReturn("hash_from");
        when(encryptionService.hash("5555555555554444")).thenReturn("hash_to");
        when(cardRepository.findByCardNumberHash("hash_from")).thenReturn(Optional.of(fromCard));
        when(cardRepository.findByCardNumberHash("hash_to")).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> transferService.transferBetweenOwnCards(transferRequest));

        assertEquals("To card not found", exception.getMessage());
        verify(cardRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void transferBetweenOwnCards_NotOwnCards() {
        User otherUser = User.builder()
                .id(2L)
                .username("otheruser")
                .email("other@example.com")
                .build();
        toCard.setUser(otherUser);

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("testuser");
        when(encryptionService.hash("4111111111111111")).thenReturn("hash_from");
        when(encryptionService.hash("5555555555554444")).thenReturn("hash_to");
        when(cardRepository.findByCardNumberHash("hash_from")).thenReturn(Optional.of(fromCard));
        when(cardRepository.findByCardNumberHash("hash_to")).thenReturn(Optional.of(toCard));

        OperationNotAllowedException exception = assertThrows(OperationNotAllowedException.class,
                () -> transferService.transferBetweenOwnCards(transferRequest));

        assertEquals("You can only transfer between your own cards", exception.getMessage());
        verify(cardRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void transferBetweenOwnCards_FromCardBlocked() {
        fromCard.setStatus(BankCard.CardStatus.BLOCKED);

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("testuser");
        when(encryptionService.hash("4111111111111111")).thenReturn("hash_from");
        when(encryptionService.hash("5555555555554444")).thenReturn("hash_to");
        when(cardRepository.findByCardNumberHash("hash_from")).thenReturn(Optional.of(fromCard));
        when(cardRepository.findByCardNumberHash("hash_to")).thenReturn(Optional.of(toCard));

        OperationNotAllowedException exception = assertThrows(OperationNotAllowedException.class,
                () -> transferService.transferBetweenOwnCards(transferRequest));

        assertEquals("Card is blocked. Only active cards can perform transfers", exception.getMessage());
        verify(cardRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void transferBetweenOwnCards_ToCardBlocked() {
        toCard.setStatus(BankCard.CardStatus.BLOCKED);

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("testuser");
        when(encryptionService.hash("4111111111111111")).thenReturn("hash_from");
        when(encryptionService.hash("5555555555554444")).thenReturn("hash_to");
        when(cardRepository.findByCardNumberHash("hash_from")).thenReturn(Optional.of(fromCard));
        when(cardRepository.findByCardNumberHash("hash_to")).thenReturn(Optional.of(toCard));

        OperationNotAllowedException exception = assertThrows(OperationNotAllowedException.class,
                () -> transferService.transferBetweenOwnCards(transferRequest));

        assertEquals("Card is blocked. Only active cards can perform transfers", exception.getMessage());
        verify(cardRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void transferBetweenOwnCards_FromCardExpired() {
        fromCard.setExpirationDate(LocalDate.now().minusDays(1));

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("testuser");
        when(encryptionService.hash("4111111111111111")).thenReturn("hash_from");
        when(encryptionService.hash("5555555555554444")).thenReturn("hash_to");
        when(cardRepository.findByCardNumberHash("hash_from")).thenReturn(Optional.of(fromCard));
        when(cardRepository.findByCardNumberHash("hash_to")).thenReturn(Optional.of(toCard));

        OperationNotAllowedException exception = assertThrows(OperationNotAllowedException.class,
                () -> transferService.transferBetweenOwnCards(transferRequest));

        assertEquals("Card is expired", exception.getMessage());
        verify(cardRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void transferBetweenOwnCards_ToCardExpired() {
        toCard.setExpirationDate(LocalDate.now().minusDays(1));

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("testuser");
        when(encryptionService.hash("4111111111111111")).thenReturn("hash_from");
        when(encryptionService.hash("5555555555554444")).thenReturn("hash_to");
        when(cardRepository.findByCardNumberHash("hash_from")).thenReturn(Optional.of(fromCard));
        when(cardRepository.findByCardNumberHash("hash_to")).thenReturn(Optional.of(toCard));

        OperationNotAllowedException exception = assertThrows(OperationNotAllowedException.class,
                () -> transferService.transferBetweenOwnCards(transferRequest));

        assertEquals("Card is expired", exception.getMessage());
        verify(cardRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void transferBetweenOwnCards_InsufficientFunds() {
        transferRequest.setAmount(BigDecimal.valueOf(1500));

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("testuser");
        when(encryptionService.hash("4111111111111111")).thenReturn("hash_from");
        when(encryptionService.hash("5555555555554444")).thenReturn("hash_to");
        when(cardRepository.findByCardNumberHash("hash_from")).thenReturn(Optional.of(fromCard));
        when(cardRepository.findByCardNumberHash("hash_to")).thenReturn(Optional.of(toCard));

        InsufficientFundsException exception = assertThrows(InsufficientFundsException.class,
                () -> transferService.transferBetweenOwnCards(transferRequest));

        assertEquals("Insufficient funds", exception.getMessage());
        verify(cardRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }


    @Test
    void transferBetweenOwnCards_ZeroAmount() {
        transferRequest.setAmount(BigDecimal.ZERO);

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("testuser");
        when(encryptionService.hash("4111111111111111")).thenReturn("hash_from");
        when(encryptionService.hash("5555555555554444")).thenReturn("hash_to");
        when(cardRepository.findByCardNumberHash("hash_from")).thenReturn(Optional.of(fromCard));
        when(cardRepository.findByCardNumberHash("hash_to")).thenReturn(Optional.of(toCard));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction transaction = invocation.getArgument(0);
            transaction.setId(1L);
            return transaction;
        });

        Transaction result = transferService.transferBetweenOwnCards(transferRequest);

        assertNotNull(result);
        assertEquals(BigDecimal.ZERO, result.getAmount());
        assertEquals(BigDecimal.valueOf(1000), fromCard.getBalance());
        assertEquals(BigDecimal.valueOf(500), toCard.getBalance());

        verify(cardRepository).save(fromCard);
        verify(cardRepository).save(toCard);
        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    void transferBetweenOwnCards_NegativeAmount() {
        transferRequest.setAmount(BigDecimal.valueOf(-100));

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("testuser");
        when(encryptionService.hash("4111111111111111")).thenReturn("hash_from");
        when(encryptionService.hash("5555555555554444")).thenReturn("hash_to");
        when(cardRepository.findByCardNumberHash("hash_from")).thenReturn(Optional.of(fromCard));
        when(cardRepository.findByCardNumberHash("hash_to")).thenReturn(Optional.of(toCard));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction transaction = invocation.getArgument(0);
            transaction.setId(1L);
            return transaction;
        });

        Transaction result = transferService.transferBetweenOwnCards(transferRequest);

        assertNotNull(result);
        assertEquals(BigDecimal.valueOf(-100), result.getAmount());
        assertEquals(BigDecimal.valueOf(1100), fromCard.getBalance());
        assertEquals(BigDecimal.valueOf(400), toCard.getBalance());

        verify(cardRepository).save(fromCard);
        verify(cardRepository).save(toCard);
        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    void transferBetweenOwnCards_EmptyDescription() {
        transferRequest.setDescription("");

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("testuser");
        when(encryptionService.hash("4111111111111111")).thenReturn("hash_from");
        when(encryptionService.hash("5555555555554444")).thenReturn("hash_to");
        when(cardRepository.findByCardNumberHash("hash_from")).thenReturn(Optional.of(fromCard));
        when(cardRepository.findByCardNumberHash("hash_to")).thenReturn(Optional.of(toCard));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction transaction = invocation.getArgument(0);
            transaction.setId(1L);
            return transaction;
        });

        Transaction result = transferService.transferBetweenOwnCards(transferRequest);

        assertNotNull(result);
        assertEquals("", result.getDescription());
    }

    @Test
    void transferBetweenOwnCards_NullDescription() {
        transferRequest.setDescription(null);

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("testuser");
        when(encryptionService.hash("4111111111111111")).thenReturn("hash_from");
        when(encryptionService.hash("5555555555554444")).thenReturn("hash_to");
        when(cardRepository.findByCardNumberHash("hash_from")).thenReturn(Optional.of(fromCard));
        when(cardRepository.findByCardNumberHash("hash_to")).thenReturn(Optional.of(toCard));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction transaction = invocation.getArgument(0);
            transaction.setId(1L);
            return transaction;
        });

        Transaction result = transferService.transferBetweenOwnCards(transferRequest);

        assertNotNull(result);
        assertNull(result.getDescription());
    }


    @Test
    void transferBetweenOwnCards_NullSecurityContext() {
        SecurityContextHolder.clearContext();

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> transferService.transferBetweenOwnCards(transferRequest));

        assertNotNull(exception);
    }
}