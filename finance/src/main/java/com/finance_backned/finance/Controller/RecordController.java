package com.finance_backned.finance.Controller;

import com.finance_backned.finance.Model.Record;
import com.finance_backned.finance.Service.RecordService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/records")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class RecordController {

    private final RecordService recordService;

    @PostMapping
    public Record createRecord(@Valid @RequestBody Record record) {
        return recordService.createRecord(record);
    }

    @GetMapping
    public List<Record> getRecords(
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String categoryId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        return recordService.getRecords(userId, categoryId, type, startDate, endDate);
    }

    @GetMapping("/{id}")
    public Record getRecordById(@PathVariable String id) {
        return recordService.getRecordById(id);
    }

    @PutMapping("/{id}")
    public Record updateRecord(
            @PathVariable String id,
            @Valid @RequestBody Record record
    ) {
        return recordService.updateRecord(id, record);
    }

    @DeleteMapping("/{id}")
    public String deleteRecord(@PathVariable String id) {
        recordService.deleteRecord(id);
        return "Record deleted successfully";
    }
}
