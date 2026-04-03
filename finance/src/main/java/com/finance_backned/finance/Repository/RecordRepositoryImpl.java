package com.finance_backned.finance.Repository;

import com.finance_backned.finance.Model.Record;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
public class RecordRepositoryImpl implements RecordRepositoryCustom {

    private final MongoTemplate mongoTemplate;

    @Override
    public List<Record> findByFilters(String userId, String categoryId, String type, LocalDate startDate, LocalDate endDate) {
        List<Criteria> criteria = new ArrayList<>();

        if (userId != null && !userId.isBlank()) {
            criteria.add(Criteria.where("userId").is(userId));
        }

        if (categoryId != null && !categoryId.isBlank()) {
            criteria.add(Criteria.where("categoryId").is(categoryId));
        }

        if (type != null && !type.isBlank()) {
            criteria.add(Criteria.where("type").is(type.toLowerCase()));
        }

        if (startDate != null || endDate != null) {
            Criteria dateCriteria = Criteria.where("date");
            if (startDate != null) {
                dateCriteria = dateCriteria.gte(startDate);
            }
            if (endDate != null) {
                dateCriteria = dateCriteria.lte(endDate);
            }
            criteria.add(dateCriteria);
        }

        Query query = new Query().with(Sort.by(Sort.Direction.DESC, "date"));
        if (!criteria.isEmpty()) {
            query.addCriteria(new Criteria().andOperator(criteria.toArray(new Criteria[0])));
        }

        return mongoTemplate.find(query, Record.class);
    }
}
