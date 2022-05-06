package com.ssafy.alpaca.db.repository;

import com.ssafy.alpaca.db.document.Chat;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface ChatRepository extends MongoRepository<Chat, String> {
    @Query("{'_id': {$lt : ?0}, 'studyId': ?1 }")
    Slice<Chat> findPartByStudyId(ObjectId id, Long studyId, Pageable pageable);

    Chat findDistinctFirstByStudyIdOrderByIdDesc(Long studyId);
}
