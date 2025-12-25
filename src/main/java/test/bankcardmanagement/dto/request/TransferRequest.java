package test.bankcardmanagement.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransferRequest {

    @NotBlank(message = "From card number is required")
    @Pattern(regexp = "^\\d{16}$", message = "From card number must be 16 digits")
    private String fromCardNumber;

    @NotBlank(message = "To card number is required")
    @Pattern(regexp = "^\\d{16}$", message = "To card number must be 16 digits")
    private String toCardNumber;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    @DecimalMax(value = "100000.0", message = "Amount cannot exceed 100,000")
    private BigDecimal amount;

    @Size(max = 255, message = "Description cannot exceed 255 characters")
    private String description;
}
