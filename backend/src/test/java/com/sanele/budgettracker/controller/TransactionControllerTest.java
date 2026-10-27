package com.sanele.budgettracker.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sanele.budgettracker.dto.TransactionRequest;
import com.sanele.budgettracker.dto.TransactionResponse;
import com.sanele.budgettracker.entity.TransactionType;
import com.sanele.budgettracker.exception.ResourceNotFoundException;
import com.sanele.budgettracker.security.JwtAuthFilter;
import com.sanele.budgettracker.service.TransactionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = TransactionController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthFilter.class))
@AutoConfigureMockMvc(addFilters = false)
@WithMockUser(username = "alice")
@DisplayName("TransactionController Slice Tests")
class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TransactionService transactionService;

    private TransactionResponse sample() {
        return new TransactionResponse(5L, 10L, "Food", new BigDecimal("150.00"),
                "Groceries", LocalDate.of(2024, 3, 15), TransactionType.EXPENSE,
                LocalDateTime.of(2024, 3, 15, 10, 0));
    }

    @Test
    @DisplayName("GET /api/transactions — returns 200 and passes the month filter to the service")
    void getAll_withMonth_returns200() throws Exception {
        when(transactionService.getTransactions("alice", "2024-03", null))
                .thenReturn(List.of(sample()));

        mockMvc.perform(get("/api/transactions").param("month", "2024-03"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].categoryName").value("Food"));

        verify(transactionService).getTransactions("alice", "2024-03", null);
    }

    @Test
    @DisplayName("POST /api/transactions — returns 201 with the created transaction")
    void create_valid_returns201() throws Exception {
        TransactionRequest request = new TransactionRequest(
                10L, new BigDecimal("150.00"), "Groceries",
                LocalDate.of(2024, 3, 15), TransactionType.EXPENSE);
        when(transactionService.create(any(TransactionRequest.class), eq("alice"))).thenReturn(sample());

        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.amount").value(150.00));

        verify(transactionService).create(any(TransactionRequest.class), eq("alice"));
    }

    @Test
    @DisplayName("POST /api/transactions — returns 400 when the amount is below the minimum")
    void create_invalidAmount_returns400() throws Exception {
        TransactionRequest request = new TransactionRequest(
                10L, new BigDecimal("0.00"), "Groceries",
                LocalDate.of(2024, 3, 15), TransactionType.EXPENSE);

        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errors.amount").exists());

        verifyNoInteractions(transactionService);
    }

    @Test
    @DisplayName("POST /api/transactions — maps ResourceNotFoundException to 404")
    void create_unknownCategory_returns404() throws Exception {
        TransactionRequest request = new TransactionRequest(
                99L, new BigDecimal("150.00"), "Groceries",
                LocalDate.of(2024, 3, 15), TransactionType.EXPENSE);
        when(transactionService.create(any(), eq("alice")))
                .thenThrow(new ResourceNotFoundException("Category not found with id: 99"));

        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("DELETE /api/transactions/{id} — returns 204 on success")
    void delete_returns204() throws Exception {
        mockMvc.perform(delete("/api/transactions/5"))
                .andExpect(status().isNoContent());

        verify(transactionService).delete(5L, "alice");
    }

    @Test
    @DisplayName("GET /api/transactions/{id} — maps ResourceNotFoundException to 404")
    void getById_missing_returns404() throws Exception {
        when(transactionService.getById(eq(99L), eq("alice")))
                .thenThrow(new ResourceNotFoundException("Transaction not found with id: 99"));

        mockMvc.perform(get("/api/transactions/99"))
                .andExpect(status().isNotFound());
    }
}
