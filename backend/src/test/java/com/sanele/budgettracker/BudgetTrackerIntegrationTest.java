package com.sanele.budgettracker;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sanele.budgettracker.dto.CategoryRequest;
import com.sanele.budgettracker.dto.LoginRequest;
import com.sanele.budgettracker.dto.RegisterRequest;
import com.sanele.budgettracker.dto.TransactionRequest;
import com.sanele.budgettracker.entity.TransactionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class BudgetTrackerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String authToken;
    private Long categoryId;

    @BeforeEach
    void setUp() throws Exception {
        // Register a test user
        RegisterRequest registerRequest = new RegisterRequest(
                "testuser", "test@example.com", "password123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());

        // Login and capture token
        LoginRequest loginRequest = new LoginRequest("testuser", "password123");
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = loginResult.getResponse().getContentAsString();
        authToken = objectMapper.readTree(responseBody).get("token").asText();

        // Create a test category
        CategoryRequest categoryRequest = new CategoryRequest("Food", TransactionType.EXPENSE);
        MvcResult categoryResult = mockMvc.perform(post("/api/categories")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(categoryRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        categoryId = objectMapper.readTree(
                categoryResult.getResponse().getContentAsString()).get("id").asLong();
    }

    @Test
    void testRegisterDuplicateUsername_returns409() throws Exception {
        RegisterRequest duplicate = new RegisterRequest(
                "testuser", "another@example.com", "password123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicate)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("Username")));
    }

    @Test
    void testLoginWithWrongPassword_returns401() throws Exception {
        LoginRequest bad = new LoginRequest("testuser", "wrongpassword");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bad)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testProtectedEndpointWithoutToken_returns403() throws Exception {
        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isForbidden());
    }

    @Test
    void testCreateAndRetrieveCategory() throws Exception {
        CategoryRequest salaryCategory = new CategoryRequest("Salary", TransactionType.INCOME);

        mockMvc.perform(post("/api/categories")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(salaryCategory)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name", is("Salary")))
                .andExpect(jsonPath("$.type", is("INCOME")));

        mockMvc.perform(get("/api/categories")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(2))));
    }

    @Test
    void testCreateTransaction_andGetMonthlySummary() throws Exception {
        TransactionRequest txRequest = new TransactionRequest(
                categoryId,
                new BigDecimal("150.00"),
                "Grocery shopping",
                LocalDate.of(2024, 3, 15),
                TransactionType.EXPENSE
        );

        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(txRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.amount", is(150.0)))
                .andExpect(jsonPath("$.categoryName", is("Food")));

        mockMvc.perform(get("/api/summary/monthly?month=2024-03")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalExpenses", is(150.0)))
                .andExpect(jsonPath("$.totalIncome", is(0)))
                .andExpect(jsonPath("$.transactionCount", is(1)));
    }

    @Test
    void testDeleteTransaction() throws Exception {
        TransactionRequest txRequest = new TransactionRequest(
                categoryId,
                new BigDecimal("75.50"),
                "Restaurant",
                LocalDate.of(2024, 3, 20),
                TransactionType.EXPENSE
        );

        MvcResult result = mockMvc.perform(post("/api/transactions")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(txRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        Long txId = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(delete("/api/transactions/" + txId)
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/transactions/" + txId)
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void testCategorySummary_returnsCorrectPercentages() throws Exception {
        // Create income category
        CategoryRequest incomeCategory = new CategoryRequest("Salary", TransactionType.INCOME);
        MvcResult catResult = mockMvc.perform(post("/api/categories")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(incomeCategory)))
                .andExpect(status().isCreated())
                .andReturn();
        Long incomeCatId = objectMapper.readTree(catResult.getResponse().getContentAsString()).get("id").asLong();

        // Add transactions
        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TransactionRequest(
                                categoryId, new BigDecimal("200.00"), "Food",
                                LocalDate.of(2024, 4, 1), TransactionType.EXPENSE))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TransactionRequest(
                                incomeCatId, new BigDecimal("800.00"), "Paycheck",
                                LocalDate.of(2024, 4, 1), TransactionType.INCOME))))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/summary/by-category?month=2024-04")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].categoryName", hasItems("Food", "Salary")));
    }

    @Test
    void testFilterTransactionsByMonth() throws Exception {
        // Add transaction in March
        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TransactionRequest(
                                categoryId, new BigDecimal("100.00"), "March expense",
                                LocalDate.of(2024, 3, 10), TransactionType.EXPENSE))))
                .andExpect(status().isCreated());

        // Add transaction in April
        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TransactionRequest(
                                categoryId, new BigDecimal("200.00"), "April expense",
                                LocalDate.of(2024, 4, 5), TransactionType.EXPENSE))))
                .andExpect(status().isCreated());

        // Filter by March only
        mockMvc.perform(get("/api/transactions?month=2024-03")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].description", is("March expense")));
    }
}
