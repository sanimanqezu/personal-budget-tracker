package com.sanele.budgettracker.service;

import com.sanele.budgettracker.dto.CategoryRequest;
import com.sanele.budgettracker.dto.CategoryResponse;
import com.sanele.budgettracker.entity.Category;
import com.sanele.budgettracker.entity.TransactionType;
import com.sanele.budgettracker.entity.User;
import com.sanele.budgettracker.exception.ConflictException;
import com.sanele.budgettracker.exception.ResourceNotFoundException;
import com.sanele.budgettracker.repository.CategoryRepository;
import com.sanele.budgettracker.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CategoryService Unit Tests")
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CategoryService categoryService;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder().id(1L).username("alice").email("alice@example.com")
                .passwordHash("hashed").build();
    }

    @Test
    @DisplayName("getAllForUser — returns the user's categories mapped to responses")
    void getAllForUser_returnsMappedCategories() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(categoryRepository.findByUserId(1L)).thenReturn(List.of(
                Category.builder().id(10L).name("Food").type(TransactionType.EXPENSE).user(user).build(),
                Category.builder().id(11L).name("Salary").type(TransactionType.INCOME).user(user).build()
        ));

        List<CategoryResponse> result = categoryService.getAllForUser("alice");

        assertThat(result).hasSize(2);
        assertThat(result).extracting(CategoryResponse::name).containsExactly("Food", "Salary");
    }

    @Test
    @DisplayName("getById — returns the category when it belongs to the user")
    void getById_existing_returnsCategory() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(categoryRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(
                Category.builder().id(10L).name("Food").type(TransactionType.EXPENSE).user(user).build()));

        CategoryResponse result = categoryService.getById(10L, "alice");

        assertThat(result.id()).isEqualTo(10L);
        assertThat(result.name()).isEqualTo("Food");
    }

    @Test
    @DisplayName("getById — throws ResourceNotFoundException when the category is missing")
    void getById_missing_throwsNotFound() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(categoryRepository.findByIdAndUserId(99L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.getById(99L, "alice"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    @DisplayName("create — persists a new category for the user")
    void create_newCategory_persists() {
        CategoryRequest request = new CategoryRequest("Food", TransactionType.EXPENSE);
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(categoryRepository.existsByNameAndUserId("Food", 1L)).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> {
            Category c = inv.getArgument(0);
            c.setId(10L);
            return c;
        });

        CategoryResponse result = categoryService.create(request, "alice");

        assertThat(result.id()).isEqualTo(10L);
        assertThat(result.name()).isEqualTo("Food");
        assertThat(result.type()).isEqualTo(TransactionType.EXPENSE);
        verify(categoryRepository).save(argThat(c -> c.getUser() == user));
    }

    @Test
    @DisplayName("create — throws ConflictException when a category with the same name exists")
    void create_duplicateName_throwsConflict() {
        CategoryRequest request = new CategoryRequest("Food", TransactionType.EXPENSE);
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(categoryRepository.existsByNameAndUserId("Food", 1L)).thenReturn(true);

        assertThatThrownBy(() -> categoryService.create(request, "alice"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Food");

        verify(categoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("update — updates the existing category's name and type")
    void update_existing_updatesFields() {
        CategoryRequest request = new CategoryRequest("Groceries", TransactionType.EXPENSE);
        Category existing = Category.builder().id(10L).name("Food").type(TransactionType.EXPENSE).user(user).build();
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(categoryRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(existing));
        when(categoryRepository.save(existing)).thenReturn(existing);

        CategoryResponse result = categoryService.update(10L, request, "alice");

        assertThat(result.name()).isEqualTo("Groceries");
        assertThat(existing.getName()).isEqualTo("Groceries");
    }

    @Test
    @DisplayName("update — throws ResourceNotFoundException when the category is missing")
    void update_missing_throwsNotFound() {
        CategoryRequest request = new CategoryRequest("Groceries", TransactionType.EXPENSE);
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(categoryRepository.findByIdAndUserId(99L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.update(99L, request, "alice"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("delete — removes the category when it belongs to the user")
    void delete_existing_deletes() {
        Category existing = Category.builder().id(10L).name("Food").type(TransactionType.EXPENSE).user(user).build();
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(categoryRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(existing));

        categoryService.delete(10L, "alice");

        verify(categoryRepository).delete(existing);
    }

    @Test
    @DisplayName("delete — throws ResourceNotFoundException when the category is missing")
    void delete_missing_throwsNotFound() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(categoryRepository.findByIdAndUserId(99L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.delete(99L, "alice"))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(categoryRepository, never()).delete(any());
    }

    @Test
    @DisplayName("getAllForUser — throws UsernameNotFoundException when the user does not exist")
    void getAllForUser_unknownUser_throwsUsernameNotFound() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.getAllForUser("ghost"))
                .isInstanceOf(UsernameNotFoundException.class);

        verifyNoInteractions(categoryRepository);
    }
}
