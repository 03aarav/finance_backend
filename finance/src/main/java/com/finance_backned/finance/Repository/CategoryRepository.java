package com.finance_backned.finance.Repository;

import com.finance_backned.finance.Model.Category;
import org.springframework.data.mongodb.repository.MongoRepository;



public interface CategoryRepository extends MongoRepository<Category, String> {
}
