package com.finance_backned.finance.Repository;

import com.finance_backned.finance.Model.Record;

import java.time.LocalDate;
import java.util.List;

public interface RecordRepositoryCustom {

    List<Record> findByFilters(String userId, String categoryId, String type, LocalDate startDate, LocalDate endDate);
}
