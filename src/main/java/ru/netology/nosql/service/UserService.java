package ru.netology.nosql.service;

import java.util.List;
import java.util.Locale;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Range;
import org.springframework.stereotype.Service;

import ru.netology.nosql.dto.UserRequest;
import ru.netology.nosql.exception.EmailAlreadyExistsException;
import ru.netology.nosql.exception.UserNotFoundException;
import ru.netology.nosql.model.User;
import ru.netology.nosql.repository.UserRepository;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public List<User> findAll() {
        return userRepository.findAll();
    }

    public User findById(String id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
    }

    /**
     * Поиск пользователей. Все фильтры необязательные и комбинируются через «И».
     *
     * @param name   часть имени (без учёта регистра)
     * @param minAge минимальный возраст включительно
     * @param maxAge максимальный возраст включительно
     */
    public List<User> search(String name, Integer minAge, Integer maxAge) {
        boolean hasName = name != null && !name.isBlank();
        boolean hasAge = minAge != null || maxAge != null;

        if (minAge != null && maxAge != null && minAge > maxAge) {
            throw new IllegalArgumentException("minAge must be less than or equal to maxAge");
        }
        Range<Integer> ageRange = Range.closed(
                minAge != null ? minAge : 0,
                maxAge != null ? maxAge : Integer.MAX_VALUE);

        if (hasName && hasAge) {
            return userRepository.findByNameContainingIgnoreCaseAndAgeBetween(name.trim(), ageRange);
        }
        if (hasName) {
            return userRepository.findByNameContainingIgnoreCase(name.trim());
        }
        if (hasAge) {
            return userRepository.findByAgeBetween(ageRange);
        }
        return userRepository.findAll();
    }

    public List<User> findByAge(int age) {
        return userRepository.findByAge(age);
    }

    public User create(UserRequest request) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyExistsException(email);
        }
        User user = new User(request.name().trim(), email, request.age());
        return save(user);
    }

    public User update(String id, UserRequest request) {
        User user = findById(id);
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmailAndIdNot(email, id)) {
            throw new EmailAlreadyExistsException(email);
        }
        user.setName(request.name().trim());
        user.setEmail(email);
        user.setAge(request.age());
        return save(user);
    }

    public void delete(String id) {
        if (!userRepository.existsById(id)) {
            throw new UserNotFoundException(id);
        }
        userRepository.deleteById(id);
    }

    private User save(User user) {
        try {
            return userRepository.save(user);
        } catch (DuplicateKeyException e) {
            // Гонка двух запросов с одинаковым email: срабатывает уникальный индекс
            throw new EmailAlreadyExistsException(user.getEmail());
        }
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
