package com.finance_backned.finance.Service;

import com.finance_backned.finance.Model.Record;

import java.time.LocalDate;
import java.util.List;

public interface RecordService {

    Record createRecord(Record record);

    List<Record> getRecords(String userId, String categoryId, String type, LocalDate startDate, LocalDate endDate);

    Record getRecordById(String recordId);

    Record updateRecord(String recordId, Record record);

    void deleteRecord(String recordId);
}
