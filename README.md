Демонстрационный проект для применения инструментов ИБ

# Библиотека

Веб-приложение для обмена книгами в общий доступ. REST-бэкенд на Java 11 (Spring Boot),
View — статические HTML/CSS/JS, хранилище метаданных — PostgreSQL, файлы — на диске.

## Запуск

```bash
docker compose up -d          # PostgreSQL на порту 5433 (БД library/library/library)
mvn spring-boot:run           # http://localhost:8081
```

Тестовый модератор создаётся при первом старте: `moderator` / `moderator123`
(переопределяется `MODERATOR_LOGIN` и `MODERATOR_PASSWORD`). Обычного пользователя
заводит форма «Регистрация» в интерфейсе.

## Архитектура (MVCS)

| Слой | Классы |
|------|--------|
| View | `src/main/resources/static/index.html`, `app.js`, `style.css` |
| Controller | `web/BookController`, `web/AuthController`, `web/ModerationController`, `web/CsrfController`, `web/HealthController`, `web/ApiExceptionHandler`, `web/SecurityConfig` |
| Service | `service/BookService`, `service/UserService`, `service/ModerationService`, `service/StorageService` |
| Model | `model/Book`, `model/User`, `model/Favorite`, `repo/*Repository` |
| Фоновое | `web/BackupJob` (резервное копирование), `web/HealthController#monitor` (мониторинг) |

## API

| Метод | Путь | Доступ |
|--------|------|--------|
| GET | `/api/books?title=` | все (список и поиск по названию) |
| GET | `/api/books/{id}` | все |
| GET | `/api/books/{id}/download` | все |
| POST | `/api/books` (`title`, `author`, `description`, `file`) | авторизованные |
| PUT | `/api/books/{id}/like` | авторизованные (переключение лайка) |
| DELETE | `/api/moderation/books/{id}` | только MODERATOR |
| POST | `/api/auth/register`, `/api/auth/login`, `/api/auth/logout` | все / авторизованные |
| GET | `/api/auth/me`, `/api/auth/csrf`, `/api/health` | все |

Изменяющие запросы требуют CSRF-токен из `/api/auth/csrf` в заголовке `X-CSRF-TOKEN`.

## Ограничения и безопасность

- принимаются только PDF, EPUB, FB2; файл больше 50 МБ отклоняется (413).
- скачивание и поиск доступны всем, загрузка и лайки — только авторизованным,
  удаление — только пользователю с ролью `MODERATOR`.
- пароли хранятся в виде BCrypt-хеша в поле `User.passwordHash` (колонка `users.password_hash`),
  вход только по паролю, сессия в HTTP-сессии с защитой от фиксации идентификатора сессии.
- при регистрации пароль должен содержать буквы верхнего и нижнего регистра, цифру
  и служебный символ (дополнительно не менее 10 символов).
- при загрузке считается SHA-256; при скачивании хеш сверяется, при несовпадении
  запрос отклоняется (500) и ошибка пишется в лог; хеш не отдаётся в API и не
  показывается в интерфейсе.
- операции загрузки и удаления выполняются в транзакции, файлы удаляются вместе с записями.
- `BackupJob` по расписанию (`app.backup.cron`, по умолчанию 03:00) делает `pg_dump`
  и копирует файлы в `data/backup/<метка>`, `HealthController` раз в минуту проверяет
  БД и отдаёт состояние в `/api/health`, ошибки логируются.

## Настройки

Переопределяются переменными окружения или аргументами: `SERVER_PORT`, `DB_HOST`,
`DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`, `STORAGE_DIR`, `BACKUP_DIR`,
`BACKUP_CRON`, `MODERATOR_LOGIN`, `MODERATOR_PASSWORD` (см. `application.properties`).
