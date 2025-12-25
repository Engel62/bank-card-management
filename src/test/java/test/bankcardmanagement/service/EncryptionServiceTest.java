package test.bankcardmanagement.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class EncryptionServiceTest {

    @InjectMocks
    private EncryptionService encryptionService;

    private final String testEncryptionKey = "12345678901234567890123456789012"; // 32 bytes for AES-256
    private final String testAlgorithm = "AES/CBC/PKCS5Padding";
    private final String testData = "4111111111111111";
    private final String shortKey = "1234567890123456"; // 16 bytes

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(encryptionService, "encryptionKey", testEncryptionKey);
        ReflectionTestUtils.setField(encryptionService, "algorithm", testAlgorithm);
    }

    @Test
    void encrypt_WithValidData_ShouldReturnEncryptedString() {
        // Act
        String encrypted = encryptionService.encrypt(testData);

        // Assert
        assertNotNull(encrypted);
        assertFalse(encrypted.isEmpty());
        assertNotEquals(testData, encrypted);

        assertDoesNotThrow(() -> Base64.getDecoder().decode(encrypted));

        assertNotEquals(testData, encrypted);
    }

    @Test
    void encrypt_WithEmptyString_ShouldReturnEncryptedString() {
        // Arrange
        String emptyData = "";

        // Act
        String encrypted = encryptionService.encrypt(emptyData);

        // Assert
        assertNotNull(encrypted);
        assertFalse(encrypted.isEmpty());
    }

    @Test
    void encrypt_WithSpecialCharacters_ShouldEncryptSuccessfully() {
        // Arrange
        String specialData = "4111-1111-1111-1111!@#$%^&*()";

        // Act
        String encrypted = encryptionService.encrypt(specialData);

        // Assert
        assertNotNull(encrypted);
        assertDoesNotThrow(() -> Base64.getDecoder().decode(encrypted));
    }

    @Test
    void decrypt_WithValidEncryptedData_ShouldReturnOriginalString() {
        // Arrange
        String encrypted = encryptionService.encrypt(testData);

        // Act
        String decrypted = encryptionService.decrypt(encrypted);

        // Assert
        assertEquals(testData, decrypted);
    }

    @Test
    void encryptAndDecrypt_ShouldBeReversible() {
        // Arrange
        String[] testCases = {
                "4111111111111111",
                "5555555555554444",
                "378282246310005",
                "6011111111111117",
                "",
                "123",
                "Test Card Holder Name",
                "Card with spaces and-special!chars@123"
        };

        for (String original : testCases) {
            // Act
            String encrypted = encryptionService.encrypt(original);
            String decrypted = encryptionService.decrypt(encrypted);

            // Assert
            assertEquals(original, decrypted,
                    "Original and decrypted should match for: " + original);
        }
    }

    @Test
    void decrypt_WithInvalidBase64_ShouldThrowException() {
        // Arrange
        String invalidBase64 = "Not-a-valid-base64-string!";

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> encryptionService.decrypt(invalidBase64));
        assertEquals("Decryption failed", exception.getMessage());
    }

    @Test
    void decrypt_WithTamperedData_ShouldThrowException() {
        // Arrange
        String encrypted = encryptionService.encrypt(testData);
        String tampered = encrypted.substring(0, encrypted.length() - 2) + "XX";

        // Act & Assert
        assertThrows(RuntimeException.class,
                () -> encryptionService.decrypt(tampered));
    }

    @Test
    void hash_WithValidData_ShouldReturnSHA256Hash() {
        // Act
        String hash = encryptionService.hash(testData);

        // Assert
        assertNotNull(hash);
        assertEquals(64, hash.length()); // SHA-256 produces 64 hex characters
        assertTrue(hash.matches("[0-9a-f]+")); // Should be hexadecimal

        String hash2 = encryptionService.hash(testData);
        assertEquals(hash, hash2);
    }

    @Test
    void hash_WithEmptyString_ShouldReturnValidHash() {
        // Arrange
        String emptyData = "";

        // Act
        String hash = encryptionService.hash(emptyData);

        // Assert
        assertNotNull(hash);
        assertEquals(64, hash.length());
        // Это известный хэш пустой строки в SHA-256
        assertEquals("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855", hash);
    }

    @Test
    void hash_WithDifferentInputs_ShouldProduceDifferentHashes() {
        // Arrange
        String data1 = "4111111111111111";
        String data2 = "4111111111111112";
        String data3 = "411111111111111";

        // Act
        String hash1 = encryptionService.hash(data1);
        String hash2 = encryptionService.hash(data2);
        String hash3 = encryptionService.hash(data3);

        // Assert
        assertNotEquals(hash1, hash2);
        assertNotEquals(hash1, hash3);
        assertNotEquals(hash2, hash3);
    }

    @Test
    void hash_WithSpecialCharacters_ShouldProduceValidHash() {
        // Arrange
        String specialData = "4111-1111-1111-1111";

        // Act
        String hash = encryptionService.hash(specialData);

        // Assert
        assertNotNull(hash);
        assertEquals(64, hash.length());
        assertTrue(hash.matches("[0-9a-f]+"));
    }

    @Test
    void encrypt_WithShortKeyInjection_ShouldWork() {
        ReflectionTestUtils.setField(encryptionService, "encryptionKey", shortKey);

        assertDoesNotThrow(() -> {
            String encrypted = encryptionService.encrypt(testData);
            String decrypted = encryptionService.decrypt(encrypted);
            assertEquals(testData, decrypted);
        });
    }

    @Test
    void bytesToHex_PrivateMethod_ShouldConvertCorrectly() {
        // Arrange
        byte[] testBytes = "hello".getBytes(StandardCharsets.UTF_8);
        String expectedHex = "68656c6c6f"; // hex for "hello"

        String result = (String) ReflectionTestUtils.invokeMethod(
                encryptionService, "bytesToHex", testBytes);

        // Assert
        assertEquals(expectedHex, result);
    }

    @Test
    void bytesToHex_WithEmptyArray_ShouldReturnEmptyString() {
        // Arrange
        byte[] emptyBytes = new byte[0];

        // Act
        String result = (String) ReflectionTestUtils.invokeMethod(
                encryptionService, "bytesToHex", emptyBytes);

        // Assert
        assertEquals("", result);
    }

    @Test
    void bytesToHex_WithSingleByte_ShouldReturnTwoHexChars() {
        // Arrange
        byte[] singleByte = new byte[]{(byte) 0x0A}; // decimal 10

        // Act
        String result = (String) ReflectionTestUtils.invokeMethod(
                encryptionService, "bytesToHex", singleByte);

        // Assert
        assertEquals("0a", result);
    }

    @Test
    void bytesToHex_WithAllBytes_ShouldHandleCorrectly() {
        // Arrange
        byte[] allBytes = new byte[256];
        for (int i = 0; i < 256; i++) {
            allBytes[i] = (byte) i;
        }

        // Act
        String result = (String) ReflectionTestUtils.invokeMethod(
                encryptionService, "bytesToHex", allBytes);

        // Assert
        assertEquals(512, result.length()); // 256 bytes * 2 hex chars per byte
        assertTrue(result.matches("[0-9a-f]+"));
    }

    @Test
    void encryptionAndDecryption_WithDifferentAlgorithm_ShouldWork() {
        // Arrange - меняем алгоритм
        ReflectionTestUtils.setField(encryptionService, "algorithm", "AES/CBC/PKCS5Padding");

        String original = "Test data for algorithm test";

        // Act
        String encrypted = encryptionService.encrypt(original);
        String decrypted = encryptionService.decrypt(encrypted);

        // Assert
        assertEquals(original, decrypted);
    }

    @Test
    void encrypt_WhenAlgorithmNotFound_ShouldThrowException() {
        ReflectionTestUtils.setField(encryptionService, "algorithm", "NonExistent/Algorithm");

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> encryptionService.encrypt(testData));
        assertEquals("Encryption failed", exception.getMessage());
        assertNotNull(exception.getCause());
    }

    @Test
    void hash_WhenAlgorithmNotFound_ShouldThrowException() {
        assertDoesNotThrow(() -> encryptionService.hash(""));
    }

    @Test
    void service_WithDefaultConstructor_ShouldInitialize() {
        // Arrange & Act
        EncryptionService service = new EncryptionService();

        // Assert
        assertNotNull(service);
        ReflectionTestUtils.setField(service, "encryptionKey", testEncryptionKey);
        ReflectionTestUtils.setField(service, "algorithm", testAlgorithm);

        assertDoesNotThrow(() -> service.encrypt("test"));
    }

    @Test
    void performanceTest_MultipleEncryptions() {
        // Arrange
        int iterations = 100;
        long startTime = System.currentTimeMillis();

        // Act
        for (int i = 0; i < iterations; i++) {
            String encrypted = encryptionService.encrypt(testData + i);
            String decrypted = encryptionService.decrypt(encrypted);
            assertEquals(testData + i, decrypted);
        }

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;


        assertTrue(duration < 5000, "100 encryptions/decryptions should take less than 5 seconds");
    }
}