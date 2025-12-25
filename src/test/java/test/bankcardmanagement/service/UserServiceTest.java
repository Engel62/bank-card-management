package test.bankcardmanagement.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import test.bankcardmanagement.dto.request.UserCreateRequest;
import test.bankcardmanagement.entity.Role;
import test.bankcardmanagement.entity.User;
import test.bankcardmanagement.repository.UserRepository;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private User testUser;
    private UserCreateRequest userCreateRequest;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .password("encodedPassword123")
                .firstName("John")
                .lastName("Doe")
                .role(Role.ROLE_USER)
                .enabled(true)
                .build();

        userCreateRequest = UserCreateRequest.builder()
                .username("testuser")
                .email("test@example.com")
                .password("password123")
                .firstName("John")
                .lastName("Doe")
                .role(Role.ROLE_USER)
                .build();
    }

    @Test
    void getAllUsers_ShouldReturnAllUsers() {
        User user2 = User.builder()
                .id(2L)
                .username("user2")
                .email("user2@example.com")
                .build();
        List<User> users = Arrays.asList(testUser, user2);

        when(userRepository.findAll()).thenReturn(users);

        List<User> result = userService.getAllUsers();

        assertEquals(2, result.size());
        assertEquals("testuser", result.get(0).getUsername());
        assertEquals("user2", result.get(1).getUsername());
        verify(userRepository).findAll();
    }

    @Test
    void getAllUsers_WhenNoUsers_ShouldReturnEmptyList() {
        when(userRepository.findAll()).thenReturn(List.of());

        List<User> result = userService.getAllUsers();

        assertTrue(result.isEmpty());
        verify(userRepository).findAll();
    }

    @Test
    void getUserById_WithValidId_ShouldReturnUser() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        User result = userService.getUserById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("testuser", result.getUsername());
        verify(userRepository).findById(1L);
    }

    @Test
    void getUserById_WithInvalidId_ShouldThrowException() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> userService.getUserById(999L));

        assertEquals("User not found with id: 999", exception.getMessage());
        verify(userRepository).findById(999L);
    }

    @Test
    void getUserById_WithNullId_ShouldThrowException() {
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> userService.getUserById(null));

        assertEquals("User not found with id: null", exception.getMessage());
        verify(userRepository).findById(null);
    }

    @Test
    void createUser_WithValidRequest_ShouldCreateUser() {
        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword123");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        User result = userService.createUser(userCreateRequest);

        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
        assertEquals("test@example.com", result.getEmail());
        assertEquals("encodedPassword123", result.getPassword());
        assertTrue(result.isEnabled());
        verify(userRepository).existsByUsername("testuser");
        verify(userRepository).existsByEmail("test@example.com");
        verify(passwordEncoder).encode("password123");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void createUser_WithExistingUsername_ShouldThrowException() {
        when(userRepository.existsByUsername("testuser")).thenReturn(true);

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> userService.createUser(userCreateRequest));

        assertEquals("Username already exists", exception.getMessage());
        verify(userRepository).existsByUsername("testuser");
        verify(userRepository, never()).existsByEmail(anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void createUser_WithExistingEmail_ShouldThrowException() {
        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> userService.createUser(userCreateRequest));

        assertEquals("Email already exists", exception.getMessage());
        verify(userRepository).existsByUsername("testuser");
        verify(userRepository).existsByEmail("test@example.com");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void createUser_WithAdminRole_ShouldCreateAdminUser() {
        userCreateRequest.setRole(Role.ROLE_ADMIN);
        testUser.setRole(Role.ROLE_ADMIN);

        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword123");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        User result = userService.createUser(userCreateRequest);

        assertNotNull(result);
        assertEquals(Role.ROLE_ADMIN, result.getRole());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void createUser_WithNullFields_ShouldHandleGracefully() {
        UserCreateRequest minimalRequest = UserCreateRequest.builder()
                .username("minimal")
                .email("minimal@example.com")
                .password("pass")
                .role(Role.ROLE_USER)
                .build();

        User minimalUser = User.builder()
                .id(1L)
                .username("minimal")
                .email("minimal@example.com")
                .password("encodedPass")
                .role(Role.ROLE_USER)
                .enabled(true)
                .build();

        when(userRepository.existsByUsername("minimal")).thenReturn(false);
        when(userRepository.existsByEmail("minimal@example.com")).thenReturn(false);
        when(passwordEncoder.encode("pass")).thenReturn("encodedPass");
        when(userRepository.save(any(User.class))).thenReturn(minimalUser);

        User result = userService.createUser(minimalRequest);

        assertNotNull(result);
        assertNull(result.getFirstName());
        assertNull(result.getLastName());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void updateUser_WithValidRequest_ShouldUpdateUser() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.existsByEmail("updated@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        userCreateRequest.setEmail("updated@example.com");
        userCreateRequest.setFirstName("Jane");
        userCreateRequest.setLastName("Smith");
        userCreateRequest.setRole(Role.ROLE_ADMIN);

        User result = userService.updateUser(1L, userCreateRequest);

        assertNotNull(result);
        assertEquals("updated@example.com", result.getEmail());
        assertEquals("Jane", result.getFirstName());
        assertEquals("Smith", result.getLastName());
        assertEquals(Role.ROLE_ADMIN, result.getRole());
        verify(userRepository).findById(1L);
        verify(userRepository).existsByEmail("updated@example.com");
        verify(userRepository).save(testUser);
    }

    @Test
    void updateUser_WithSameEmail_ShouldUpdateWithoutEmailCheck() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        User result = userService.updateUser(1L, userCreateRequest);

        assertNotNull(result);
        verify(userRepository).findById(1L);
        verify(userRepository, never()).existsByEmail(anyString());
        verify(userRepository).save(testUser);
    }


    @Test
    void updateUser_WithExistingEmail_ShouldThrowException() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

        userCreateRequest.setEmail("existing@example.com");

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> userService.updateUser(1L, userCreateRequest));

        assertEquals("Email already exists", exception.getMessage());
        verify(userRepository).findById(1L);
        verify(userRepository).existsByEmail("existing@example.com");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void updateUser_UserNotFound_ShouldThrowException() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> userService.updateUser(999L, userCreateRequest));

        assertEquals("User not found with id: 999", exception.getMessage());
        verify(userRepository).findById(999L);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void deleteUser_WithValidId_ShouldDeleteUser() {
        when(userRepository.existsById(1L)).thenReturn(true);

        userService.deleteUser(1L);

        verify(userRepository).existsById(1L);
        verify(userRepository).deleteById(1L);
    }

    @Test
    void deleteUser_UserNotFound_ShouldThrowException() {
        when(userRepository.existsById(999L)).thenReturn(false);

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> userService.deleteUser(999L));

        assertEquals("User not found with id: 999", exception.getMessage());
        verify(userRepository).existsById(999L);
        verify(userRepository, never()).deleteById(anyLong());
    }

    @Test
    void deleteUser_WithNullId_ShouldThrowException() {
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> userService.deleteUser(null));

        assertEquals("User not found with id: null", exception.getMessage());
        verify(userRepository).existsById(null);
    }


    @Test
    void updateUser_WithNoChanges_ShouldStillSave() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        User result = userService.updateUser(1L, userCreateRequest);

        assertNotNull(result);
        verify(userRepository).save(testUser);
    }

    @Test
    void createUser_WithSpecialCharacters_ShouldCreateUser() {
        UserCreateRequest specialRequest = UserCreateRequest.builder()
                .username("user-name_123")
                .email("user.name+test@example.com")
                .password("Pass@123!")
                .firstName("John-O'Conner")
                .lastName("Doe-Smith")
                .role(Role.ROLE_USER)
                .build();

        User specialUser = User.builder()
                .id(1L)
                .username("user-name_123")
                .email("user.name+test@example.com")
                .password("encodedPass")
                .firstName("John-O'Conner")
                .lastName("Doe-Smith")
                .role(Role.ROLE_USER)
                .enabled(true)
                .build();

        when(userRepository.existsByUsername("user-name_123")).thenReturn(false);
        when(userRepository.existsByEmail("user.name+test@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Pass@123!")).thenReturn("encodedPass");
        when(userRepository.save(any(User.class))).thenReturn(specialUser);

        User result = userService.createUser(specialRequest);

        assertNotNull(result);
        assertEquals("user-name_123", result.getUsername());
        assertEquals("user.name+test@example.com", result.getEmail());
        assertEquals("John-O'Conner", result.getFirstName());
        verify(userRepository).save(any(User.class));
    }
}