package ru.netology.nosql.controller;

import java.net.URI;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import ru.netology.nosql.dto.UserRequest;
import ru.netology.nosql.model.User;
import ru.netology.nosql.service.UserService;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Список пользователей с необязательными фильтрами:
     * {@code /api/users?name=ann&minAge=18&maxAge=30}.
     */
    @GetMapping
    public List<User> getUsers(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) @Min(0) @Max(150) Integer minAge,
            @RequestParam(required = false) @Min(0) @Max(150) Integer maxAge) {
        return userService.search(name, minAge, maxAge);
    }

    @GetMapping("/{id}")
    public User getUser(@PathVariable String id) {
        return userService.findById(id);
    }

    /** Поиск по точному возрасту: {@code /api/users/age/25}. */
    @GetMapping("/age/{age}")
    public List<User> getUsersByAge(@PathVariable @Min(0) @Max(150) int age) {
        return userService.findByAge(age);
    }

    @PostMapping
    public ResponseEntity<User> createUser(@Valid @RequestBody UserRequest request) {
        User created = userService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.getId())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{id}")
    public User updateUser(@PathVariable String id, @Valid @RequestBody UserRequest request) {
        return userService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable String id) {
        userService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
