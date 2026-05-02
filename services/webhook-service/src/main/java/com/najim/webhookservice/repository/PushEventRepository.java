package com.najim.webhookservice.repository;

import com.najim.webhookservice.model.PushEvent;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface PushEventRepository extends MongoRepository<PushEvent, String> {

}
