package test.bankcardmanagement.service;



import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import test.bankcardmanagement.dto.request.CardCreateRequest;
import test.bankcardmanagement.entity.BankCard;
import test.bankcardmanagement.entity.Role;
import test.bankcardmanagement.entity.User;
import test.bankcardmanagement.exception.CardNotFoundException;
import test.bankcardmanagement.exception.OperationNotAllowedException;
import test.bankcardmanagement.exception.ValidationException;
import test.bankcardmanagement.repository.BankCardRepository;
import test.bankcardmanagement.repository.UserRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CardServiceTest {

    @Mock
    private BankCardRepository cardRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EncryptionService encryptionService;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private CardService cardService;

    private User testUser;
    private BankCard testCard;
    private CardCreateRequest validRequest;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .role(Role.ROLE_USER)
                .build();

        testCard = BankCard.builder()
                .id(1L)
                .cardNumberEncrypted("encrypted123456789012")
                .cardNumberHash("hash123456789012")
                .lastFourDigits("9012")
                .cardHolderName("Test User")
                .expirationDate(LocalDate.now().plusYears(3))
                .status(BankCard.CardStatus.ACTIVE)
                .balance(BigDecimal.valueOf(1000))
                .user(testUser)
                .build();

        validRequest = CardCreateRequest.builder()
                .cardNumber("4111111111111111") // Valid test card
                .cardHolderName("Test User")
                .expirationDate(LocalDate.now().plusYears(3))
                .userId(1L)
                .initialBalance(BigDecimal.valueOf(1000))
                .build();

        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    void createCard_WithValidRequest_ShouldCreateCard() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(encryptionService.encrypt(any())).thenReturn("encrypted");
        when(encryptionService.hash(any())).thenReturn("hash");
        when(cardRepository.existsByCardNumberHash("hash")).thenReturn(false);
        when(cardRepository.save(any())).thenReturn(testCard);

        // Act
        var result = cardService.createCard(validRequest);

        // Assert
        assertNotNull(result);
        assertEquals("**** **** **** 9012", result.getMaskedCardNumber());
        verify(cardRepository).save(any(BankCard.class));
    }

    @Test
    void createCard_WithInvalidCardNumber_ShouldThrowException() {
        // Arrange
        validRequest.setCardNumber("4111111111111112"); // Invalid Luhn

        // Act & Assert
        assertThrows(ValidationException.class,
                () -> cardService.createCard(validRequest));
    }

    @Test
    void getCardById_WhenCardExistsAndUserHasAccess_ShouldReturnCard() {

        when(cardRepository.findById(1L)).thenReturn(Optional.of(testCard));
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("testuser");
        when(authentication.getAuthorities())
                .thenReturn(List.of(new SimpleGrantedAuthority("ROLE_USER")));

        // Act
        var result = cardService.getCardById(1L);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
        verify(cardRepository).findById(1L);
    }

    @Test
    void getCardById_WhenCardNotFound_ShouldThrowException() {
        // Arrange
        when(cardRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(CardNotFoundException.class, () -> cardService.getCardById(1L));
    }

    @Test
    void getCardById_WhenUserNoAccess_ShouldThrowException() {
        // Arrange
        when(cardRepository.findById(1L)).thenReturn(Optional.of(testCard));
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("otheruser");
        when(authentication.getAuthorities())
                .thenReturn(List.of(new SimpleGrantedAuthority("ROLE_USER")));

        // Act & Assert
        assertThrows(OperationNotAllowedException.class,
                () -> cardService.getCardById(1L));
    }

    @Test
    void getUserCards_ShouldReturnUserCards() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Page<BankCard> cardPage = new PageImpl<>(List.of(testCard), pageable, 1);

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(cardRepository.findByUser(eq(testUser), any(Pageable.class))).thenReturn(cardPage);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("testuser");

        // Act
        var result = cardService.getUserCards(pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void updateCardStatus_WhenValid_ShouldUpdateStatus() {
        // Arrange
        when(cardRepository.findById(1L)).thenReturn(Optional.of(testCard));
        when(cardRepository.save(any())).thenReturn(testCard);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("testuser");
        when(authentication.getAuthorities())
                .thenReturn(List.of(new SimpleGrantedAuthority("ROLE_USER")));

        // Act
        var result = cardService.updateCardStatus(1L, BankCard.CardStatus.BLOCKED);

        // Assert
        assertNotNull(result);
        verify(cardRepository).save(any(BankCard.class));
    }

    @Test
    void deleteCard_WhenAdmin_ShouldDeleteCard() {
        // Arrange
        when(cardRepository.findById(1L)).thenReturn(Optional.of(testCard));
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getAuthorities())
                .thenReturn(List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

        // Act
        cardService.deleteCard(1L);

        // Assert
        verify(cardRepository).delete(testCard);
    }
}