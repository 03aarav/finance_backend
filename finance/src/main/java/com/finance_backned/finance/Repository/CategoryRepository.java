package com.finance_backned.finance.Repository;

import com.finance_backned.finance.Model.Category;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface CategoryRepository extends MongoRepository<Category, String> {

    List<Category> findByUserIdOrUserIdIsNull(String userId);

    boolean existsByNameAndUserId(String name, String userId);
}
