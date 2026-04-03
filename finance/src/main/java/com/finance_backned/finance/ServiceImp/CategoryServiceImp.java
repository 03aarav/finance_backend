package com.finance_backned.finance.ServiceImp;

import com.finance_backned.finance.ExceptionHandler.BadRequestException;
import com.finance_backned.finance.ExceptionHandler.ResourceNotFoundException;
import com.finance_backned.finance.ExceptionHandler.UnauthorizedException;
import com.finance_backned.finance.Model.Category;
import com.finance_backned.finance.Repository.CategoryRepository;
import com.finance_backned.finance.Service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryServiceImp implements CategoryService {

    private final CategoryRepository categoryRepository;

    @Override
    public Category createCategory(Category category, String userId) {
        category.setUserId(userId);

        if (categoryRepository.existsByNameAndUserId(category.getName(), userId)) {
            throw new BadRequestException("Category already exists");
        }

        return categoryRepository.save(category);
    }

    @Override
    public List<Category> getAllCategories(String userId) {
        return categoryRepository.findByUserIdOrUserIdIsNull(userId);
    }

    @Override
    public void deleteCategory(String categoryId, String userId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        if (category.getUserId() == null) {
            throw new BadRequestException("Cannot delete default category");
        }

        if (!category.getUserId().equals(userId)) {
            throw new UnauthorizedException("Unauthorized");
        }

        categoryRepository.deleteById(categoryId);
    }
}
