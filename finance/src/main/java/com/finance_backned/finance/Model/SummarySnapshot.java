package com.finance_backned.finance.Model;



import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;

@Document(collection = "summary_snapshots")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@CompoundIndex(
        name = "bucket_unique",
        def = "{'periodType': 1, 'periodKey': 1, 'categoryId': 1}",
        unique = true
)
public class SummarySnapshot {

    @Id
    private String id;

    private String periodType;   // "week" | "month" | "year"
    private String periodKey;    // "2025-W11" | "2025-03" | "2025"
    private String categoryId;   // actual category id or "__all__"
    private BigDecimal income;
    private BigDecimal expense;
    private BigDecimal net;
    private int recordCount;
}
