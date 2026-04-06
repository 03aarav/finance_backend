package com.finance_backned.finance.ServiceImp;


import com.finance_backned.finance.Model.Record;
import com.finance_backned.finance.Model.SummarySnapshot;
import com.finance_backned.finance.Util.PeriodKeyUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SummaryService {

    private final MongoTemplate mongoTemplate;

    // call on CREATE
    public void addRecord(Record record) {
        applyDelta(record, 1);
    }

    // call on DELETE
    public void removeRecord(Record record) {
        applyDelta(record, -1);
    }

    // call on UPDATE — pass old record and new record
    public void replaceRecord(Record oldRecord, Record newRecord) {
        applyDelta(oldRecord, -1);  // subtract old values
        applyDelta(newRecord, +1);  // add new values
    }

    private void applyDelta(Record record, int multiplier) {
        boolean isIncome = record.getType().equalsIgnoreCase("income");

        BigDecimal income  = isIncome
                ? record.getAmount().multiply(BigDecimal.valueOf(multiplier))
                : BigDecimal.ZERO;

        BigDecimal expense = isIncome
                ? BigDecimal.ZERO
                : record.getAmount().multiply(BigDecimal.valueOf(multiplier));

        BigDecimal net = income.subtract(expense);

        List<String[]> buckets = PeriodKeyUtil.getPeriodBuckets(record.getDate());

        for (String[] bucket : buckets) {
            String periodType = bucket[0];
            String periodKey  = bucket[1];

            // update category-specific bucket
            upsertBucket(periodType, periodKey, record.getCategoryId(),
                    income, expense, net, multiplier);

            // update __all__ bucket
            upsertBucket(periodType, periodKey, "__all__",
                    income, expense, net, multiplier);
        }
    }

    private void upsertBucket(String periodType, String periodKey,
                              String categoryId, BigDecimal income,
                              BigDecimal expense, BigDecimal net,
                              int countDelta) {
        Query query = new Query(
                Criteria.where("periodType").is(periodType)
                        .and("periodKey").is(periodKey)
                        .and("categoryId").is(categoryId)
        );

        Update update = new Update()
                .inc("income",      income.doubleValue())
                .inc("expense",     expense.doubleValue())
                .inc("net",         net.doubleValue())
                .inc("recordCount", countDelta)
                .setOnInsert("periodType", periodType)
                .setOnInsert("periodKey",  periodKey)
                .setOnInsert("categoryId", categoryId);

        mongoTemplate.upsert(query, update,
                com.finance_backned.finance.Model.SummarySnapshot.class);
    }

    // ── READ METHODS ──────────────────────────────────────────────

    public SummarySnapshot getOverallSummary() {
        // single __all__ year-to-date doesn't exist as one row,
        // so we aggregate all "year" + "__all__" rows
        Query query = new Query(
                Criteria.where("periodType").is("year")
                        .and("categoryId").is("__all__")
        );
        List<SummarySnapshot> rows = mongoTemplate.find(query, SummarySnapshot.class);

        BigDecimal totalIncome  = rows.stream()
                .map(SummarySnapshot::getIncome)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalExpense = rows.stream()
                .map(SummarySnapshot::getExpense)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return SummarySnapshot.builder()
                .periodType("all")
                .periodKey("all-time")
                .categoryId("__all__")
                .income(totalIncome)
                .expense(totalExpense)
                .net(totalIncome.subtract(totalExpense))
                .build();
    }

    public List<SummarySnapshot> getCategoryTotals(String periodType, String periodKey) {
        // e.g. periodType="month", periodKey="2025-03"
        // returns one row per category (excludes __all__)
        Query query = new Query(
                Criteria.where("periodType").is(periodType)
                        .and("periodKey").is(periodKey)
                        .and("categoryId").ne("__all__")
        );
        return mongoTemplate.find(query, SummarySnapshot.class);
    }

    public List<SummarySnapshot> getTrends(String periodType) {
        // returns all __all__ rows for a period type, ordered by key
        // e.g. all monthly totals → monthly trend chart
        Query query = new Query(
                Criteria.where("periodType").is(periodType)
                        .and("categoryId").is("__all__")
        );
        query.with(Sort.by(Sort.Direction.ASC, "periodKey"));
        return mongoTemplate.find(query, SummarySnapshot.class);
    }

    public List<Record> getRecentActivity(int limit) {
        Query query = new Query()
                .with(Sort.by(Sort.Direction.DESC, "date"))
                .limit(limit);
        return mongoTemplate.find(query, Record.class);
    }

    public String recalculateAll() {
        // step 1 — wipe all existing summaries
        mongoTemplate.dropCollection(SummarySnapshot.class);

        // step 2 — fetch every record in the collection
        List<Record> allRecords = mongoTemplate.findAll(Record.class);

        int processed = 0;
        int skipped = 0;

        for (Record record : allRecords) {
            // skip dirty data — records inserted directly with null fields
            if (record.getType() == null || record.getAmount() == null || record.getDate() == null) {
                skipped++;
                continue;
            }

            // skip unknown types — guard against bad strings in DB
            String type = record.getType().toLowerCase();
            if (!type.equals("income") && !type.equals("expense")) {
                skipped++;
                continue;
            }

            // normalize and reprocess
            record.setType(type);
            addRecord(record);
            processed++;
        }

        return String.format(
                "Recalculation complete. Processed: %d records. Skipped: %d invalid records.",
                processed, skipped
        );
    }
}
