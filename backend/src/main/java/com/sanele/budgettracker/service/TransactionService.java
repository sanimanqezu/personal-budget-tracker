package com.sanele.budgettracker.service;

import com.sanele.budgettracker.dto.TransactionRequest;
import com.sanele.budgettracker.dto.TransactionResponse;
import com.sanele.budgettracker.entity.Category;
import com.sanele.budgettracker.entity.Transaction;
import com.sanele.budgettracker.entity.TransactionType;
import com.sanele.budgettracker.entity.User;
import com.sanele.budgettracker.exception.ResourceNotFoundException;
import com.sanele.budgettracker.repository.CategoryRepository;
import com.sanele.budgettracker.repository.TransactionRepository;
import com.sanele.budgettracker.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<TransactionResponse> getTransactions(String username, String month, TransactionType type) {
        User user = getUser(username);

        if (month != null) {
            YearMonth yearMonth = YearMonth.parse(month);
            LocalDate start = yearMonth.atDay(1);
            LocalDate end = yearMonth.atEndOfMonth();

            if (type != null) {
                return transactionRepository
                        .findByUserIdAndTypeAndTransactionDateBetweenOrderByTransactionDateDesc(
                                user.getId(), type, start, end)
                        .stream()
                        .map(TransactionResponse::from)
                        .toList();
            }
            return transactionRepository
                    .findByUserIdAndTransactionDateBetweenOrderByTransactionDateDesc(
                            user.getId(), start, end)
                    .stream()
                    .map(TransactionResponse::from)
                    .toList();
        }

        return transactionRepository
                .findByUserIdOrderByTransactionDateDesc(user.getId())
                .stream()
                .map(TransactionResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public TransactionResponse getById(Long id, String username) {
        User user = getUser(username);
        Transaction transaction = transactionRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with id: " + id));
        return TransactionResponse.from(transaction);
    }

    @Transactional
    public TransactionResponse create(TransactionRequest request, String username) {
        User user = getUser(username);
        Category category = categoryRepository.findByIdAndUserId(request.categoryId(), user.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Category not found with id: " + request.categoryId()));

        Transaction transaction = Transaction.builder()
                .user(user)
                .category(category)
                .amount(request.amount())
                .description(request.description())
                .transactionDate(request.transactionDate())
                .type(request.type())
                .build();

        return TransactionResponse.from(transactionRepository.save(transaction));
    }

    @Transactional
    public void delete(Long id, String username) {
        User user = getUser(username);
        Transaction transaction = transactionRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with id: " + id));
        transactionRepository.delete(transaction);
    }

    private User getUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }
}
