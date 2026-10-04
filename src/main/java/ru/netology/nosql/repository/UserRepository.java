package ru.netology.nosql.repository;

import java.util.List;

import org.springframework.data.domain.Range;
import org.springframework.data.mongodb.repository.MongoRepository;

import ru.netology.nosql.model.User;

public interface UserRepository extends MongoRepository<User, String> {

    /** Поиск по части имени без учёта регистра. */
    List<User> findByNameContainingIgnoreCase(String name);

    /** Поиск по точному возрасту. */
    List<User> findByAge(Integer age);

    /** Поиск по диапазону возраста (границы задаёт {@link Range}). */
    List<User> findByAgeBetween(Range<Integer> ageRange);

    /** Поиск по части имени и диапазону возраста одновременно. */
    List<User> findByNameContainingIgnoreCaseAndAgeBetween(String name, Range<Integer> ageRange);

    boolean existsByEmail(String email);

    boolean existsByEmailAndIdNot(String email, String id);
}
