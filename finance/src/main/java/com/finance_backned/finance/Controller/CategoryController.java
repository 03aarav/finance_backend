package com.finance_backned.finance.Controller;

import com.finance_backned.finance.Model.Category;
import com.finance_backned.finance.Service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @PostMapping
    public Category createCategory(
            @RequestBody Category category,
            @RequestHeader("userId") String userId
    ) {
        return categoryService.createCategory(category, userId);
    }

    @GetMapping
    public List<Category> getCategories(@RequestHeader("userId") String userId) {
        return categoryService.getAllCategories(userId);
    }

    @DeleteMapping("/{id}")
    public String deleteCategory(
            @PathVariable String id,
            @RequestHeader("userId") String userId
    ) {
        categoryService.deleteCategory(id, userId);
        return "Category deleted successfully";
    }
}
