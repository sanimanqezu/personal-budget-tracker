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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TransactionService Unit Tests")
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private TransactionService transactionService;

    private User user;
    private Category category;

    @BeforeEach
    void setUp() {
        user = User.builder().id(1L).username("alice").email("alice@example.com")
                .passwordHash("hashed").build();
        category = Category.builder().id(10L).name("Food").type(TransactionType.EXPENSE).user(user).build();
    }

    private Transaction tx(Long id, BigDecimal amount, LocalDate date) {
        return Transaction.builder().id(id).user(user).category(category)
                .amount(amount).description("desc").transactionDate(date)
                .type(TransactionType.EXPENSE).build();
    }

    @Test
    @DisplayName("getTransactions — returns all transactions when no month filter is supplied")
    void getTransactions_noMonth_returnsAll() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(transactionRepository.findByUserIdOrderByTransactionDateDesc(1L))
                .thenReturn(List.of(tx(1L, new BigDecimal("10.00"), LocalDate.of(2024, 3, 1))));

        List<TransactionResponse> result = transactionService.getTransactions("alice", null, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).categoryName()).isEqualTo("Food");
        verify(transactionRepository).findByUserIdOrderByTransactionDateDesc(1L);
    }

    @Test
    @DisplayName("getTransactions — filters by month when a month is supplied")
    void getTransactions_withMonth_filtersByDateRange() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(transactionRepository.findByUserIdAndTransactionDateBetweenOrderByTransactionDateDesc(
                1L, LocalDate.of(2024, 3, 1), LocalDate.of(2024, 3, 31)))
                .thenReturn(List.of(tx(1L, new BigDecimal("10.00"), LocalDate.of(2024, 3, 15))));

        List<TransactionResponse> result = transactionService.getTransactions("alice", "2024-03", null);

        assertThat(result).hasSize(1);
        verify(transactionRepository).findByUserIdAndTransactionDateBetweenOrderByTransactionDateDesc(
                1L, LocalDate.of(2024, 3, 1), LocalDate.of(2024, 3, 31));
    }

    @Test
    @DisplayName("getTransactions — filters by month and type when both are supplied")
    void getTransactions_withMonthAndType_filtersBoth() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(transactionRepository.findByUserIdAndTypeAndTransactionDateBetweenOrderByTransactionDateDesc(
                1L, TransactionType.EXPENSE, LocalDate.of(2024, 3, 1), LocalDate.of(2024, 3, 31)))
                .thenReturn(List.of(tx(1L, new BigDecimal("10.00"), LocalDate.of(2024, 3, 15))));

        List<TransactionResponse> result =
                transactionService.getTransactions("alice", "2024-03", TransactionType.EXPENSE);

        assertThat(result).hasSize(1);
        verify(transactionRepository).findByUserIdAndTypeAndTransactionDateBetweenOrderByTransactionDateDesc(
                1L, TransactionType.EXPENSE, LocalDate.of(2024, 3, 1), LocalDate.of(2024, 3, 31));
    }

    @Test
    @DisplayName("getById — throws ResourceNotFoundException when the transaction is missing")
    void getById_missing_throwsNotFound() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(transactionRepository.findByIdAndUserId(99L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.getById(99L, "alice"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    @DisplayName("create — persists a transaction against an existing category")
    void create_validCategory_persists() {
        TransactionRequest request = new TransactionRequest(
                10L, new BigDecimal("150.00"), "Groceries",
                LocalDate.of(2024, 3, 15), TransactionType.EXPENSE);
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(categoryRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(category));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> {
            Transaction t = inv.getArgument(0);
            t.setId(5L);
            return t;
        });

        TransactionResponse result = transactionService.create(request, "alice");

        assertThat(result.id()).isEqualTo(5L);
        assertThat(result.amount()).isEqualByComparingTo("150.00");
        assertThat(result.categoryName()).isEqualTo("Food");
        verify(transactionRepository).save(argThat(t -> t.getCategory() == category && t.getUser() == user));
    }

    @Test
    @DisplayName("create — throws ResourceNotFoundException when the category does not belong to the user")
    void create_unknownCategory_throwsNotFound() {
        TransactionRequest request = new TransactionRequest(
                99L, new BigDecimal("150.00"), "Groceries",
                LocalDate.of(2024, 3, 15), TransactionType.EXPENSE);
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(categoryRepository.findByIdAndUserId(99L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.create(request, "alice"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");

        verify(transactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("delete — throws ResourceNotFoundException when the transaction is missing")
    void delete_missing_throwsNotFound() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(transactionRepository.findByIdAndUserId(99L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.delete(99L, "alice"))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(transactionRepository, never()).delete(any());
    }
}
