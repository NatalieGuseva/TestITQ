# Document Service

Backend-сервис для управления документами с workflow утверждения.

## Стек

- Java 21 + Spring Boot 3.2
- PostgreSQL 15 (Docker Compose)
- JPA/Hibernate + Liquibase
- Maven (multi-module)

## Структура проекта

```
TestITQ/
├── docker-compose.yml          # PostgreSQL
├── pom.xml                     # корневой Maven POM
├── service/                    # основной сервис
│   └── src/main/
│       ├── java/org/example/documents/
│       │   ├── controller/     # REST контроллеры
│       │   ├── service/        # бизнес-логика
│       │   ├── repository/     # JPA репозитории
│       │   ├── entity/         # JPA сущности
│       │   ├── dto/            # request/response DTO
│       │   ├── exception/      # обработка ошибок
│       │   └── worker/         # фоновые процессы
│       └── resources/
│           ├── application.yml
│           └── db/changelog/   # Liquibase миграции
└── generator/                  # утилита генерации документов
```

## Быстрый старт

### 1. Поднять PostgreSQL

```bash
docker-compose up -d
```

Проверить что БД готова:
```bash
docker-compose ps
# documents_postgres должен быть healthy
```

### 2. Запустить сервис

```bash
cd service
mvn spring-boot:run
```

Сервис стартует на `http://localhost:8080`

Swagger UI: `http://localhost:8080/swagger-ui.html`

### 3. Запустить утилиту генерации

```bash
cd generator
mvn package -DskipTests

# Создать 100 документов
java -jar target/document-generator-0.0.1-SNAPSHOT.jar --count=100 --author=test-user

# Или задать количество в конфиге generator/src/main/resources/application.yml
# и запустить без параметров
java -jar target/document-generator.jar
```

## API

| Метод | URL | Описание |
|-------|-----|----------|
| POST | `/api/v1/documents` | Создать документ |
| GET | `/api/v1/documents/{id}` | Получить документ с историей |
| GET | `/api/v1/documents/batch?ids=1,2,3` | Пакетное получение |
| GET | `/api/v1/documents/search` | Поиск по фильтрам |
| POST | `/api/v1/documents/submit` | Отправить на согласование |
| POST | `/api/v1/documents/approve` | Утвердить |
| POST | `/api/v1/documents/{id}/concurrency-test` | Тест конкурентного утверждения |

## Фоновые процессы

Два воркера работают автоматически после старта сервиса:

- **SubmitWorker** — каждые 10 сек берёт пачку DRAFT документов и переводит в SUBMITTED
- **ApproveWorker** — каждые 15 сек берёт пачку SUBMITTED документов и переводит в APPROVED

Настройка в `application.yml`:
```yaml
app:
  worker:
    batch-size: 50
    submit:
      delay-ms: 10000
    approve:
      delay-ms: 15000
```

## Мониторинг прогресса по логам

### Прогресс генерации (утилита)
```
=== Запуск генерации: всего документов к созданию = 500 ===
Прогресс: создано 100/500 документов
Прогресс: создано 200/500 документов
...
=== Генерация завершена: успешно=500, ошибок=0, время=3420 мс ===
```

### Ход фоновой обработки (сервис)
```
SubmitWorker: найдено 500 документов в DRAFT, обрабатываю пачку из 50
SubmitWorker: пачка обработана за 234 мс — успешно=50, ошибок=0, осталось примерно=450

ApproveWorker: найдено 450 документов в SUBMITTED, обрабатываю пачку из 50
ApproveWorker: пачка обработана за 312 мс — успешно=50, ошибок=0, осталось примерно=400
```

## Запуск тестов

```bash
cd service
mvn test
```

Тесты используют Testcontainers — Docker должен быть запущен.

## Опциональные улучшения

### Обработка 5000+ id в одном запросе

- Разбивать список на чанки по 500-1000 и обрабатывать параллельно через `CompletableFuture`
- Использовать `@Async` с пулом потоков вместо последовательной обработки
- Для approve применить `SELECT FOR UPDATE SKIP LOCKED` чтобы воркеры не блокировали API запросы
- Рассмотреть переход на реактивный стек (WebFlux + R2DBC) для неблокирующей обработки

### Реестр утверждений как отдельная система

**Вариант 1 — отдельная БД:**
- Вынести `ApprovalRegistry` в отдельный DataSource
- Использовать распределённые транзакции (XA/2PC) или паттерн Saga
- При недоступности реестра — откатывать approve и возвращать `REGISTRY_ERROR`

**Вариант 2 — отдельный HTTP-сервис:**
- `ApprovalRegistryService` делает HTTP-вызов вместо записи в локальную БД
- Использовать паттерн Outbox: сначала писать событие в локальную таблицу,
  затем фоновый процесс отправляет его в реестр-сервис
- Это гарантирует at-least-once доставку даже при временной недоступности реестра