package com.finance_backned.finance.Service;

import com.finance_backned.finance.Model.Category;

import java.util.List;

public interface CategoryService {

    Category createCategory(Category category, String userId);

    List<Category> getAllCategories(String userId);

    void deleteCategory(String categoryId, String userId);
}
