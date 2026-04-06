package com.finance_backned.finance.Model;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "categories")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Category {

    @Id
    private String id;

    @NotBlank(message = "Category name is required")
    private String name;

    @NotBlank(message = "Category type is required")
    private String type;

    private String createdBy;  // renamed from userId — clearer intent
}