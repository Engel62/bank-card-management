package test.bankcardmanagement.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import test.bankcardmanagement.dto.request.CardCreateRequest;
import test.bankcardmanagement.dto.response.CardResponse;
import test.bankcardmanagement.entity.BankCard;
import test.bankcardmanagement.service.CardService;

@RestController
@RequestMapping("/api/cards")
@RequiredArgsConstructor
@Tag(name = "Cards", description = "Bank Card Management APIs")
@SecurityRequirement(name = "bearerAuth")
public class CardController {

    private final CardService cardService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a new card (Admin only)")
    public ResponseEntity<CardResponse> createCard(@Valid @RequestBody CardCreateRequest request) {
        return ResponseEntity.ok(cardService.createCard(request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get card by ID")
    public ResponseEntity<CardResponse> getCardById(@PathVariable Long id) {
        return ResponseEntity.ok(cardService.getCardById(id));
    }

    @GetMapping("/my")
    @Operation(summary = "Get user's own cards with pagination")
    public ResponseEntity<Page<CardResponse>> getMyCards(
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(cardService.getUserCards(pageable));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all cards (Admin only)")
    public ResponseEntity<Page<CardResponse>> getAllCards(
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(cardService.getAllCards(pageable));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update card status")
    public ResponseEntity<CardResponse> updateCardStatus(
            @PathVariable Long id,
            @RequestParam BankCard.CardStatus status) {
        return ResponseEntity.ok(cardService.updateCardStatus(id, status));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete card (Admin only)")
    public ResponseEntity<Void> deleteCard(@PathVariable Long id) {
        cardService.deleteCard(id);
        return ResponseEntity.noContent().build();
    }
}