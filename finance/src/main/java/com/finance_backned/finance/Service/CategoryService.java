package com.finance_backned.finance.Service;

import com.finance_backned.finance.Model.Category;
import java.util.List;

public interface CategoryService {

    Category createCategory(Category category);

    List<Category> getAllCategories();

    Category getCategoryById(String categoryId);

    void deleteCategory(String categoryId);
}