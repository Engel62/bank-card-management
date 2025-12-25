package test.bankcardmanagement.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import test.bankcardmanagement.dto.request.TransferRequest;
import test.bankcardmanagement.entity.Transaction;
import test.bankcardmanagement.service.TransferService;

@RestController
@RequestMapping("/api/transfers")
@RequiredArgsConstructor
@Tag(name = "Transfers", description = "Money Transfer APIs")
@SecurityRequirement(name = "bearerAuth")
public class TransferController {

    private final TransferService transferService;

    @PostMapping("/own")
    @Operation(summary = "Transfer between own cards")
    public ResponseEntity<Transaction> transferBetweenOwnCards(
            @Valid @RequestBody TransferRequest request) {
        return ResponseEntity.ok(transferService.transferBetweenOwnCards(request));
    }
}