package test.bankcardmanagement.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import test.bankcardmanagement.entity.BankCard;
import test.bankcardmanagement.entity.Transaction;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Optional<Transaction> findByTransactionId(String transactionId);

    Page<Transaction> findByFromCard_User_Id(Long userId, Pageable pageable);

    Page<Transaction> findByToCard_User_Id(Long userId, Pageable pageable);
}