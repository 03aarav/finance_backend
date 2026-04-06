package com.finance_backned.finance.ServiceImp;

import com.finance_backned.finance.ExceptionHandler.BadRequestException;
import com.finance_backned.finance.ExceptionHandler.ResourceNotFoundException;
import com.finance_backned.finance.Model.Category;
import com.finance_backned.finance.Repository.CategoryRepository;
import com.finance_backned.finance.Service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryServiceImp implements CategoryService {

    private final CategoryRepository categoryRepository;

    @Override
    public Category createCategory(Category category) {
        validateType(category.getType());

        // auto-set createdBy from the authenticated user
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        category.setCreatedBy(auth.getName());
        category.setType(category.getType().toLowerCase());

        return categoryRepository.save(category);
    }

    @Override
    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    @Override
    public Category getCategoryById(String categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
    }

    @Override
    public void deleteCategory(String categoryId) {
        Category category = getCategoryById(categoryId);
        categoryRepository.delete(category);
    }

    private void validateType(String type) {
        if (type == null || type.isBlank()) {
            throw new BadRequestException("Category type is required");
        }
        String normalized = type.toLowerCase();
        if (!normalized.equals("income") && !normalized.equals("expense")) {
            throw new BadRequestException("Category type must be either income or expense");
        }
    }
}