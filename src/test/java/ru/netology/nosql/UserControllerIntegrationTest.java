package ru.netology.nosql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import ru.netology.nosql.model.User;
import ru.netology.nosql.repository.UserRepository;

@SpringBootTest
@AutoConfigureMockMvc
class UserControllerIntegrationTest {

    private static final String USERS = "/api/users";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void clearCollection() {
        userRepository.deleteAll();
    }

    @Test
    void fullCrudScenario() throws Exception {
        // CREATE
        String createdJson = mockMvc.perform(post(USERS)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userJson("Anna", "Anna@Example.com", 25)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", notNullValue()))
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.name").value("Anna"))
                .andExpect(jsonPath("$.email").value("anna@example.com"))
                .andExpect(jsonPath("$.age").value(25))
                .andReturn().getResponse().getContentAsString();
        String id = objectMapper.readTree(createdJson).get("id").asText();

        // READ
        mockMvc.perform(get(USERS + "/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.name").value("Anna"));
        mockMvc.perform(get(USERS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        // UPDATE
        mockMvc.perform(put(USERS + "/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userJson("Anna Smith", "anna.smith@example.com", 26)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.name").value("Anna Smith"))
                .andExpect(jsonPath("$.email").value("anna.smith@example.com"))
                .andExpect(jsonPath("$.age").value(26));
        User stored = userRepository.findById(id).orElseThrow();
        assertThat(stored.getName()).isEqualTo("Anna Smith");
        assertThat(stored.getAge()).isEqualTo(26);

        // DELETE
        mockMvc.perform(delete(USERS + "/" + id))
                .andExpect(status().isNoContent());
        assertThat(userRepository.existsById(id)).isFalse();
        mockMvc.perform(get(USERS + "/" + id))
                .andExpect(status().isNotFound());
    }

    @Test
    void createReturnsLocationOfNewUser() throws Exception {
        String json = mockMvc.perform(post(USERS)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userJson("Ivan", "ivan@example.com", 30)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String id = objectMapper.readTree(json).get("id").asText();

        mockMvc.perform(get(USERS + "/" + id))
                .andExpect(status().isOk());
        mockMvc.perform(post(USERS)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userJson("Petr", "petr@example.com", 31)))
                .andExpect(header().string("Location", matchesPattern(".*/api/users/[0-9a-f]{24}$")));
    }

    @Test
    void searchByNameAndAge() throws Exception {
        userRepository.save(new User("Anna", "anna@example.com", 20));
        userRepository.save(new User("Joanna", "joanna@example.com", 35));
        userRepository.save(new User("Boris", "boris@example.com", 25));

        mockMvc.perform(get(USERS).param("name", "ANNA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name", containsInAnyOrder("Anna", "Joanna")));

        mockMvc.perform(get(USERS).param("minAge", "20").param("maxAge", "25"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name", containsInAnyOrder("Anna", "Boris")));

        mockMvc.perform(get(USERS).param("name", "anna").param("minAge", "30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name", containsInAnyOrder("Joanna")));

        mockMvc.perform(get(USERS + "/age/25"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name", containsInAnyOrder("Boris")));

        mockMvc.perform(get(USERS).param("name", "nobody"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void searchTreatsNameAsPlainText() throws Exception {
        userRepository.save(new User("Anna", "anna@example.com", 20));

        mockMvc.perform(get(USERS).param("name", ".*"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void createWithInvalidDataReturnsValidationErrors() throws Exception {
        mockMvc.perform(post(USERS)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userJson(" ", "not-an-email", -5)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.details.name", notNullValue()))
                .andExpect(jsonPath("$.details.email").value("email must be a valid email address"))
                .andExpect(jsonPath("$.details.age").value("age must be >= 0"));

        mockMvc.perform(post(USERS)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Oleg\",\"email\":\"oleg@example.com\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.age").value("age is required"));

        mockMvc.perform(post(USERS)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userJson("Oleg", "oleg@example.com", 200)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.age").value("age must be <= 150"));

        assertThat(userRepository.count()).isZero();
    }

    @Test
    void malformedJsonReturnsBadRequest() throws Exception {
        mockMvc.perform(post(USERS)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Oleg\", "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Request body is missing or malformed JSON"));
    }

    @Test
    void duplicateEmailReturnsConflict() throws Exception {
        User existing = userRepository.save(new User("Anna", "anna@example.com", 20));
        User other = userRepository.save(new User("Boris", "boris@example.com", 30));

        mockMvc.perform(post(USERS)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userJson("Another Anna", "ANNA@example.com", 22)))
                .andExpect(status().isConflict());

        mockMvc.perform(put(USERS + "/" + other.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userJson("Boris", "anna@example.com", 30)))
                .andExpect(status().isConflict());

        // Пользователь может сохранить свой же email
        mockMvc.perform(put(USERS + "/" + existing.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userJson("Anna", "anna@example.com", 21)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.age").value(21));
    }

    @Test
    void unknownIdReturnsNotFound() throws Exception {
        String missingId = "000000000000000000000000";

        mockMvc.perform(get(USERS + "/" + missingId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("User with id '" + missingId + "' not found"));
        mockMvc.perform(put(USERS + "/" + missingId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userJson("Anna", "anna@example.com", 20)))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete(USERS + "/" + missingId))
                .andExpect(status().isNotFound());
    }

    @Test
    void invalidSearchParamsReturnBadRequest() throws Exception {
        mockMvc.perform(get(USERS).param("minAge", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.minAge", notNullValue()));
        mockMvc.perform(get(USERS).param("minAge", "40").param("maxAge", "30"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("minAge must be less than or equal to maxAge"));
        mockMvc.perform(get(USERS).param("maxAge", "abc"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get(USERS + "/age/500"))
                .andExpect(status().isBadRequest());
    }

    private String userJson(String name, String email, int age) throws Exception {
        JsonNode node = objectMapper.createObjectNode()
                .put("name", name)
                .put("email", email)
                .put("age", age);
        return objectMapper.writeValueAsString(node);
    }
}
