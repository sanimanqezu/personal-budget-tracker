package com.sanele.budgettracker.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sanele.budgettracker.dto.CategoryRequest;
import com.sanele.budgettracker.dto.CategoryResponse;
import com.sanele.budgettracker.entity.TransactionType;
import com.sanele.budgettracker.exception.ConflictException;
import com.sanele.budgettracker.exception.ResourceNotFoundException;
import com.sanele.budgettracker.security.JwtAuthFilter;
import com.sanele.budgettracker.service.CategoryService;
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

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = CategoryController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthFilter.class))
@AutoConfigureMockMvc(addFilters = false)
@WithMockUser(username = "alice")
@DisplayName("CategoryController Slice Tests")
class CategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CategoryService categoryService;

    @Test
    @DisplayName("GET /api/categories — returns 200 with the authenticated user's categories")
    void getAll_returns200() throws Exception {
        when(categoryService.getAllForUser("alice")).thenReturn(List.of(
                new CategoryResponse(10L, "Food", TransactionType.EXPENSE)));

        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Food"));

        verify(categoryService).getAllForUser("alice");
    }

    @Test
    @DisplayName("POST /api/categories — returns 201 and delegates with the username")
    void create_valid_returns201() throws Exception {
        CategoryRequest request = new CategoryRequest("Food", TransactionType.EXPENSE);
        when(categoryService.create(any(CategoryRequest.class), eq("alice")))
                .thenReturn(new CategoryResponse(10L, "Food", TransactionType.EXPENSE));

        mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.type").value("EXPENSE"));

        verify(categoryService).create(any(CategoryRequest.class), eq("alice"));
    }

    @Test
    @DisplayName("POST /api/categories — returns 400 when the name is blank")
    void create_invalid_returns400() throws Exception {
        CategoryRequest request = new CategoryRequest("", TransactionType.EXPENSE);

        mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"));

        verifyNoInteractions(categoryService);
    }

    @Test
    @DisplayName("POST /api/categories — maps ConflictException to 409")
    void create_duplicate_returns409() throws Exception {
        CategoryRequest request = new CategoryRequest("Food", TransactionType.EXPENSE);
        when(categoryService.create(any(), eq("alice")))
                .thenThrow(new ConflictException("Category 'Food' already exists"));

        mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    @DisplayName("GET /api/categories/{id} — maps ResourceNotFoundException to 404")
    void getById_missing_returns404() throws Exception {
        when(categoryService.getById(eq(99L), eq("alice")))
                .thenThrow(new ResourceNotFoundException("Category not found with id: 99"));

        mockMvc.perform(get("/api/categories/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("DELETE /api/categories/{id} — returns 204 on success")
    void delete_returns204() throws Exception {
        mockMvc.perform(delete("/api/categories/10"))
                .andExpect(status().isNoContent());

        verify(categoryService).delete(10L, "alice");
    }
}
