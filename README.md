# Домашнее задание «Нереляционные (NoSQL) базы данных»

Spring Boot приложение с MongoDB: CRUD-операции над коллекцией `users`.

## Стек

- Java 17+, Maven
- Spring Boot 3.5 (Web, Data MongoDB, Validation)
- MongoDB (локально или MongoDB Atlas)
- Тесты: JUnit 5, MockMvc, встроенная MongoDB (flapdoodle) — для тестов сервер MongoDB не нужен

## Структура

```
src/main/java/ru/netology/nosql
├── NoSqlApplication.java              точка входа
├── config/MongoConnectionChecker.java проверка подключения к MongoDB (ping) при старте
├── model/User.java                    документ @Document("users"): id, name, email, age
├── dto/UserRequest.java               тело запроса + правила валидации
├── repository/UserRepository.java     MongoRepository<User, String> + поиск по имени и возрасту
├── service/UserService.java           бизнес-логика, проверка уникальности email
├── controller/UserController.java     REST API /api/users
└── exception/                         обработка ошибок (400 / 404 / 409) в едином формате
```

## Модель `User`

| Поле  | Тип     | Правила                                   |
|-------|---------|-------------------------------------------|
| id    | String  | генерируется MongoDB (`ObjectId`)         |
| name  | String  | обязательно, 2–100 символов               |
| email | String  | обязательно, корректный email, уникальный |
| age   | Integer | обязательно, от 0 до 150                  |

Email приводится к нижнему регистру; на поле создан уникальный индекс.

## Запуск

1. Запустить MongoDB на `localhost:27017` (или взять строку подключения MongoDB Atlas).
2. При необходимости указать свою строку подключения через переменную окружения:
   ```
   set MONGODB_URI=mongodb+srv://user:password@cluster.mongodb.net/netology_users
   ```
   По умолчанию используется `mongodb://localhost:27017/netology_users`
   (см. `src/main/resources/application.properties`).
3. Запустить приложение:
   ```
   mvn spring-boot:run
   ```
   При успешном подключении в логе появится строка
   `Connected to MongoDB, database 'netology_users', ping result: {"ok": 1.0}`.

## REST API

| Метод  | URL                                          | Описание                          | Ответ           |
|--------|----------------------------------------------|-----------------------------------|-----------------|
| GET    | `/api/users`                                 | все пользователи                  | 200             |
| GET    | `/api/users?name=ann&minAge=18&maxAge=30`    | поиск по части имени и возрасту   | 200 / 400       |
| GET    | `/api/users/age/{age}`                       | пользователи точного возраста     | 200 / 400       |
| GET    | `/api/users/{id}`                            | пользователь по id                | 200 / 404       |
| POST   | `/api/users`                                 | создать пользователя              | 201 / 400 / 409 |
| PUT    | `/api/users/{id}`                            | изменить пользователя             | 200 / 400 / 404 / 409 |
| DELETE | `/api/users/{id}`                            | удалить пользователя              | 204 / 404       |

Фильтры `name`, `minAge`, `maxAge` необязательные и комбинируются; поиск по имени — по подстроке без учёта регистра.

Пример запроса:

```
POST /api/users
Content-Type: application/json

{ "name": "Anna", "email": "anna@example.com", "age": 25 }
```

Пример ошибки валидации (400):

```json
{
  "timestamp": "2026-10-04T17:40:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/users",
  "details": {
    "name": "name must not be blank; name length must be between 2 and 100",
    "email": "email must be a valid email address",
    "age": "age must be >= 0"
  }
}
```

## Проверка

- **Postman:** импортировать `postman/nosql-users.postman_collection.json` и выполнить запросы по порядку
  (запрос *Create user* сохраняет `id` в переменную `userId`).
- **Автотесты:** `mvn test` — 9 интеграционных тестов покрывают полный CRUD-сценарий, поиск, валидацию,
  конфликт email и 404. Тесты поднимают встроенную MongoDB 7.0 (при первом запуске она скачивается).
