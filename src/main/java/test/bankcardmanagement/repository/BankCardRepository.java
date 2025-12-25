package test.bankcardmanagement.repository;


import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import test.bankcardmanagement.entity.BankCard;
import test.bankcardmanagement.entity.User;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;



@Repository
public interface BankCardRepository extends JpaRepository<BankCard, Long> {

    Optional<BankCard> findByCardNumberHash(String cardNumberHash);

    boolean existsByCardNumberHash(String cardNumberHash);

    Page<BankCard> findByUser(User user, Pageable pageable);

    Page<BankCard> findByUserId(Long userId, Pageable pageable);

    Page<BankCard> findByStatus(BankCard.CardStatus status, Pageable pageable);

    List<BankCard> findByExpirationDateBefore(LocalDate date);

    // Простой поиск по статусу и пользователю (опционально)
    Page<BankCard> findByUserIdAndStatus(Long userId, BankCard.CardStatus status, Pageable pageable);
}