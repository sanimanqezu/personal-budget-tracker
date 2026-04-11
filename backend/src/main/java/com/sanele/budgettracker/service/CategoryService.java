package com.sanele.budgettracker.service;

import com.sanele.budgettracker.dto.CategoryRequest;
import com.sanele.budgettracker.dto.CategoryResponse;
import com.sanele.budgettracker.entity.Category;
import com.sanele.budgettracker.entity.User;
import com.sanele.budgettracker.exception.ConflictException;
import com.sanele.budgettracker.exception.ResourceNotFoundException;
import com.sanele.budgettracker.repository.CategoryRepository;
import com.sanele.budgettracker.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllForUser(String username) {
        User user = getUser(username);
        return categoryRepository.findByUserId(user.getId())
                .stream()
                .map(CategoryResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public CategoryResponse getById(Long id, String username) {
        User user = getUser(username);
        Category category = categoryRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));
        return CategoryResponse.from(category);
    }

    @Transactional
    public CategoryResponse create(CategoryRequest request, String username) {
        User user = getUser(username);
        if (categoryRepository.existsByNameAndUserId(request.name(), user.getId())) {
            throw new ConflictException("Category '" + request.name() + "' already exists");
        }
        Category category = Category.builder()
                .name(request.name())
                .type(request.type())
                .user(user)
                .build();
        return CategoryResponse.from(categoryRepository.save(category));
    }

    @Transactional
    public CategoryResponse update(Long id, CategoryRequest request, String username) {
        User user = getUser(username);
        Category category = categoryRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));
        category.setName(request.name());
        category.setType(request.type());
        return CategoryResponse.from(categoryRepository.save(category));
    }

    @Transactional
    public void delete(Long id, String username) {
        User user = getUser(username);
        Category category = categoryRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));
        categoryRepository.delete(category);
    }

    private User getUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }
}
