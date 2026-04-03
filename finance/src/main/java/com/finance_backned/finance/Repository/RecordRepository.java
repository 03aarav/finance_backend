package com.finance_backned.finance.Repository;

import com.finance_backned.finance.Model.Record;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface RecordRepository extends MongoRepository<Record, String>, RecordRepositoryCustom {
}
