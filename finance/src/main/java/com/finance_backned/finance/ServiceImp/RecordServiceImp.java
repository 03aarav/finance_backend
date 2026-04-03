package com.finance_backned.finance.ServiceImp;

import com.finance_backned.finance.ExceptionHandler.BadRequestException;
import com.finance_backned.finance.ExceptionHandler.ResourceNotFoundException;
import com.finance_backned.finance.Model.Category;
import com.finance_backned.finance.Model.Record;
import com.finance_backned.finance.Repository.CategoryRepository;
import com.finance_backned.finance.Repository.RecordRepository;
import com.finance_backned.finance.Service.RecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RecordServiceImp implements RecordService {

    private final RecordRepository recordRepository;
    private final CategoryRepository categoryRepository;

    @Override
    public Record createRecord(Record record) {
        validateRecord(record);
        record.setType(record.getType().toLowerCase());
        return recordRepository.save(record);
    }

    @Override
    public List<Record> getRecords(String userId, String categoryId, String type, LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new BadRequestException("Start date cannot be after end date");
        }

        if (type != null && !type.isBlank()) {
            validateType(type);
        }

        if (categoryId != null && !categoryId.isBlank()) {
            validateCategory(categoryId);
        }

        return recordRepository.findByFilters(userId, categoryId, type, startDate, endDate);
    }

    @Override
    public Record getRecordById(String recordId) {
        return getRecord(recordId);
    }

    @Override
    public Record updateRecord(String recordId, Record record) {
        Record existingRecord = getRecord(recordId);
        validateRecord(record);

        existingRecord.setUserId(record.getUserId());
        existingRecord.setAmount(record.getAmount());
        existingRecord.setType(record.getType().toLowerCase());
        existingRecord.setCategoryId(record.getCategoryId());
        existingRecord.setDate(record.getDate());
        existingRecord.setNotes(record.getNotes());

        return recordRepository.save(existingRecord);
    }

    @Override
    public void deleteRecord(String recordId) {
        recordRepository.delete(getRecord(recordId));
    }

    private Record getRecord(String recordId) {
        return recordRepository.findById(recordId)
                .orElseThrow(() -> new ResourceNotFoundException("Record not found"));
    }

    private void validateRecord(Record record) {
        if (record.getUserId() == null || record.getUserId().isBlank()) {
            throw new BadRequestException("User id is required");
        }

        if (record.getType() == null || record.getType().isBlank()) {
            throw new BadRequestException("Type is required");
        }

        String normalizedType = validateType(record.getType());

        Category category = validateCategory(record.getCategoryId());
        if (!category.getType().equalsIgnoreCase(normalizedType)) {
            throw new BadRequestException("Category type must match record type");
        }
    }

    private Category validateCategory(String categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
    }

    private String validateType(String type) {
        String normalizedType = type.toLowerCase();
        if (!normalizedType.equals("income") && !normalizedType.equals("expense")) {
            throw new BadRequestException("Type must be either income or expense");
        }
        return normalizedType;
    }
}
