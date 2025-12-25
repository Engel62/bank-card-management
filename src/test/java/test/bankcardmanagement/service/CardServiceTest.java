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
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import test.bankcardmanagement.dto.request.CardCreateRequest;
import test.bankcardmanagement.dto.response.CardResponse;
import test.bankcardmanagement.entity.BankCard;
import test.bankcardmanagement.entity.Role;
import test.bankcardmanagement.entity.User;
import test.bankcardmanagement.exception.CardNotFoundException;
import test.bankcardmanagement.exception.OperationNotAllowedException;
import test.bankcardmanagement.repository.BankCardRepository;
import test.bankcardmanagement.repository.UserRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
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
                .cardNumber("4111111111111111")
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
        when(cardRepository.save(any(BankCard.class))).thenReturn(testCard);

        // Act
        CardResponse result = cardService.createCard(validRequest);

        // Assert
        assertNotNull(result);
        assertEquals("**** **** **** 9012", result.getMaskedCardNumber());
        assertEquals(BigDecimal.valueOf(1000), result.getBalance());
        assertEquals("Test User", result.getCardHolderName());
        verify(cardRepository).save(any(BankCard.class));
    }


    @Test
    void createCard_WithExistingCard_ShouldThrowException() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(encryptionService.encrypt(any())).thenReturn("encrypted");
        when(encryptionService.hash(any())).thenReturn("hash");
        when(cardRepository.existsByCardNumberHash("hash")).thenReturn(true);

        // Act & Assert
        assertThrows(RuntimeException.class, () -> cardService.createCard(validRequest));
        verify(cardRepository, never()).save(any());
    }

    @Test
    void createCard_WithNonExistingUser_ShouldThrowException() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> cardService.createCard(validRequest));
        verify(cardRepository, never()).save(any());
    }

    @Test
    void getCardById_WhenCardExistsAndUserHasAccess_ShouldReturnCard() {
        // Arrange
        when(cardRepository.findById(1L)).thenReturn(Optional.of(testCard));
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("testuser");

        Set<SimpleGrantedAuthority> authorities = Set.of(new SimpleGrantedAuthority("ROLE_USER"));
        when(authentication.getAuthorities()).thenAnswer(invocation -> authorities);

        // Act
        CardResponse result = cardService.getCardById(1L);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
        verify(cardRepository).findById(1L);
    }

    @Test
    void getCardById_WhenCardNotFound_ShouldThrowCardNotFoundException() {
        // Arrange
        when(cardRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(CardNotFoundException.class, () -> cardService.getCardById(1L));
    }

    @Test
    void getCardById_WhenUserNoAccess_ShouldThrowOperationNotAllowedException() {
        // Arrange
        when(cardRepository.findById(1L)).thenReturn(Optional.of(testCard));
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("otheruser");

        Set<SimpleGrantedAuthority> authorities = Set.of(new SimpleGrantedAuthority("ROLE_USER"));
        when(authentication.getAuthorities()).thenAnswer(invocation -> authorities);

        // Act & Assert
        assertThrows(OperationNotAllowedException.class, () -> cardService.getCardById(1L));
    }

    @Test
    void getCardById_WhenAdmin_ShouldReturnCard() {
        // Arrange
        when(cardRepository.findById(1L)).thenReturn(Optional.of(testCard));
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("admin");

        Set<SimpleGrantedAuthority> authorities = Set.of(new SimpleGrantedAuthority("ROLE_ADMIN"));
        when(authentication.getAuthorities()).thenAnswer(invocation -> authorities);

        // Act
        CardResponse result = cardService.getCardById(1L);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
    }

    @Test
    void getUserCards_ShouldReturnUserCards() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Page<BankCard> cardPage = new PageImpl<>(List.of(testCard), pageable, 1);

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(cardRepository.findByUser(testUser, pageable)).thenReturn(cardPage);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("testuser");

        // Act
        Page<CardResponse> result = cardService.getUserCards(pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("Test User", result.getContent().get(0).getCardHolderName());
    }

    @Test
    void getUserCards_WhenUserNotFound_ShouldThrowException() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.empty());
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("testuser");

        // Act & Assert
        assertThrows(RuntimeException.class, () -> cardService.getUserCards(pageable));
    }

    @Test
    void getAllCards_ShouldReturnAllCards() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Page<BankCard> cardPage = new PageImpl<>(List.of(testCard), pageable, 1);
        when(cardRepository.findAll(pageable)).thenReturn(cardPage);

        // Act
        Page<CardResponse> result = cardService.getAllCards(pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(cardRepository).findAll(pageable);
    }

    @Test
    void updateCardStatus_WhenValid_ShouldUpdateStatus() {
        // Arrange
        when(cardRepository.findById(1L)).thenReturn(Optional.of(testCard));
        when(cardRepository.save(any(BankCard.class))).thenReturn(testCard);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("testuser");

        Set<SimpleGrantedAuthority> authorities = Set.of(new SimpleGrantedAuthority("ROLE_USER"));
        when(authentication.getAuthorities()).thenAnswer(invocation -> authorities);

        // Act
        CardResponse result = cardService.updateCardStatus(1L, BankCard.CardStatus.BLOCKED);

        // Assert
        assertNotNull(result);
        verify(cardRepository).save(any(BankCard.class));
    }

    @Test
    void updateCardStatus_WhenCardExpired_ShouldThrowException() {
        // Arrange
        testCard.setExpirationDate(LocalDate.now().minusDays(1));
        when(cardRepository.findById(1L)).thenReturn(Optional.of(testCard));
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("testuser");

        Set<SimpleGrantedAuthority> authorities = Set.of(new SimpleGrantedAuthority("ROLE_USER"));
        when(authentication.getAuthorities()).thenAnswer(invocation -> authorities);

        // Act & Assert
        assertThrows(OperationNotAllowedException.class,
                () -> cardService.updateCardStatus(1L, BankCard.CardStatus.BLOCKED));
        verify(cardRepository, never()).save(any());
    }

    @Test
    void deleteCard_WhenAdmin_ShouldDeleteCard() {
        // Arrange
        when(cardRepository.findById(1L)).thenReturn(Optional.of(testCard));
        when(securityContext.getAuthentication()).thenReturn(authentication);

        Set<SimpleGrantedAuthority> authorities = Set.of(new SimpleGrantedAuthority("ROLE_ADMIN"));
        when(authentication.getAuthorities()).thenAnswer(invocation -> authorities);

        // Act
        cardService.deleteCard(1L);

        // Assert
        verify(cardRepository).delete(testCard);
    }

    @Test
    void deleteCard_WhenNotAdmin_ShouldThrowException() {
        // Arrange
        when(cardRepository.findById(1L)).thenReturn(Optional.of(testCard));
        when(securityContext.getAuthentication()).thenReturn(authentication);

        Set<SimpleGrantedAuthority> authorities = Set.of(new SimpleGrantedAuthority("ROLE_USER"));
        when(authentication.getAuthorities()).thenAnswer(invocation -> authorities);

        // Act & Assert
        assertThrows(OperationNotAllowedException.class, () -> cardService.deleteCard(1L));
        verify(cardRepository, never()).delete(any());
    }

    @Test
    void testCardNumberValidation_ValidNumbers() {

        CardCreateRequest valid1 = CardCreateRequest.builder()
                .cardNumber("4111111111111111") // Valid test card
                .cardHolderName("Test")
                .expirationDate(LocalDate.now().plusYears(1))
                .userId(1L)
                .initialBalance(BigDecimal.valueOf(100))
                .build();

        CardCreateRequest valid2 = CardCreateRequest.builder()
                .cardNumber("5555555555554444")
                .cardHolderName("Test")
                .expirationDate(LocalDate.now().plusYears(1))
                .userId(1L)
                .initialBalance(BigDecimal.valueOf(100))
                .build();

        // Act & Assert
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(encryptionService.encrypt(any())).thenReturn("encrypted");
        when(encryptionService.hash(any())).thenReturn("hash");
        when(cardRepository.existsByCardNumberHash("hash")).thenReturn(false);
        when(cardRepository.save(any())).thenReturn(testCard);

        assertDoesNotThrow(() -> {
            cardService.createCard(valid1);
            cardService.createCard(valid2);
        });
    }


}