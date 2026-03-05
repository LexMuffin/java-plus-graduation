# ExploreWithMe — Микросервисная платформа

ExploreWithMe — это микросервисное приложение для управления событиями, пользователями, запросами на участие и статистикой просмотров.

## Архитектура проекта

Проект разделен на несколько микросервисов, каждый из которых отвечает за свою доменную область, и инфраструктурные компоненты для обнаружения, конфигурации и маршрутизации.

---

## Бизнес-сервисы

| Сервис           | Описание                                                                                                    |
| ---------------- | ----------------------------------------------------------------------------------------------------------- |
| stats-server     | Хранит и предоставляет статистику по просмотрам событий                                                     |
| user-service     | Управление пользователями: создание, удаление, получение списков                                            |
| category-service | Управление категориями событий                                                                              |
| event-service    | Управление событиями: создание, обновление, публикация, получение списков (админ, приватный, публичный API) |
| request-service  | Обработка запросов на участие в событиях: создание, отмена, подтверждение и отклонение                      |

---

## Инфраструктурные компоненты

| Компонент                             | Описание                                                                                                                            | Порт |
| ------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------- | ---- |
| discovery-server (Eureka Server)      | Обнаружение сервисов. Все микросервисы регистрируются здесь для динамического обнаружения                                           | 8761 |
| config-server                         | Централизованное управление конфигурациями. Загружает YAML-конфигурации для каждого сервиса из Git-репозитория или локальных файлов | 8888 |
| gateway-server (Spring Cloud Gateway) | Шлюз для маршрутизации внешних запросов к сервисам. Использует Eureka для маршрутизации по именам сервисов                          | 8080 |

---

## Базы данных

Каждый сервис использует отдельную PostgreSQL базу данных, которые запускаются в Docker-контейнерах:

* stats-db
* user-db
* category-db
* event-db
* request-db

Все базы данных доступны внутри Docker-сети по порту **5432**.

---

## Взаимодействие между сервисами

### Обнаружение сервисов

Все сервисы регистрируются в **Eureka Server**.

### Внутреннее взаимодействие

Реализовано через **Feign-клиенты** в модуле:

```
core/interaction-api
```

### Конфигурация

Через **Config Server**.

Каждый сервис импортирует конфигурацию:

```
spring.config.import=configserver:
```

### Базы данных

Каждый сервис имеет свою **изолированную БД**.

---

# Внешний API

Доступен через **gateway-server**:

```
http://localhost:8080
```

---

# Публичные маршруты

```http
GET    /categories                 # получение списка категорий
GET    /categories/{catId}         # получение категории по ID

GET    /compilations               # получение списка подборок
GET    /compilations/{compId}      # получение подборки по ID

GET    /events                     # получение списка событий с фильтрацией
GET    /events/{id}                # получение события по ID
GET    /events/by-ids              # получение событий по списку ID
```

---

# Приватные маршруты (для авторизованных пользователей)

```http
POST   /users/{userId}/events
GET    /users/{userId}/events
GET    /users/{userId}/events/{eventId}
PATCH  /users/{userId}/events/{eventId}

POST   /users/{userId}/requests
GET    /users/{userId}/requests
PATCH  /users/{userId}/requests/{requestId}/cancel

GET    /users/{userId}/events/{eventId}/requests
PATCH  /users/{userId}/events/{eventId}/requests
```

---

# Административные маршруты

```http
POST   /admin/users
GET    /admin/users
GET    /admin/users/{userId}
DELETE /admin/users/{userId}

POST   /admin/categories
PATCH  /admin/categories/{catId}
DELETE /admin/categories/{catId}

POST   /admin/compilations
PATCH  /admin/compilations/{compId}
DELETE /admin/compilations/{compId}

GET    /admin/events
PATCH  /admin/events/{eventId}
```

---

# Внутренние маршруты (для Feign-клиентов)

```http
GET /internal/events/{id}
GET /internal/events/{eventId}/confirmed-requests
```

---

# Маршруты статистики

```http
POST /hit
GET  /stats
```
