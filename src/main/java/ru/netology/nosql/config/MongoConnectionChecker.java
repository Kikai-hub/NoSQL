package ru.netology.nosql.config;

import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

/**
 * Проверяет подключение к MongoDB при старте приложения командой {@code ping}.
 * Если база недоступна, приложение завершится с понятной ошибкой в логе.
 */
@Component
public class MongoConnectionChecker implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(MongoConnectionChecker.class);

    private final MongoTemplate mongoTemplate;

    public MongoConnectionChecker(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            Document result = mongoTemplate.executeCommand(new Document("ping", 1));
            log.info("Connected to MongoDB, database '{}', ping result: {}",
                    mongoTemplate.getDb().getName(), result.toJson());
        } catch (RuntimeException e) {
            log.error("Could not connect to MongoDB: {}", e.getMessage());
            throw e;
        }
    }
}
