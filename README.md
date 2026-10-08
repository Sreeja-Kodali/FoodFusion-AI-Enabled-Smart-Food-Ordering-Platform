# FoodFusion — Phase 1

FoodFusion is a Java 17 / Spring Boot microservices platform with a React ordering UI. Phase 1 includes authentication, food catalog and ordering services, workflow events, notifications, analytics foundations, service discovery, centralized configuration, an API Gateway, and an AI Assistant service foundation.

## Services

| Module | Responsibility | Default port |
| --- | --- | ---: |
| `config-server` | Native Spring Cloud Config Server backed by `config-repo/` | 8888 |
| `discovery-server` | Eureka registry and dashboard | 8761 |
| `api-gateway` | API routes, CORS, JWT validation, role checks, and trusted identity forwarding | 8084 |
| `auth-service` | Registration, login, BCrypt password hashes, and signed JWT issuance | 8085 |
| `product-service` | Food catalog CRUD and search backed by MongoDB | 8081 |
| `inventory-service` | Inventory and SKU availability backed by MySQL | 8083 |
| `order-service` | Order creation, inventory checks, and Avro event publishing; MySQL | 8082 |
| `workflow-service` | Consumes order events, records workflow state in MongoDB, and publishes status events | 8088 |
| `notification-service` | Consumes order status events and stores user notifications in MongoDB | 8089 |
| `analytics-service` | Consumes order status events and exposes summary/daily analytics from MongoDB | 8090 |
| `ai-assistant-service` | Menu-search assistant foundation, with an optional provider adapter | 8091 |
| `kafka-consumer` | Existing consumer that logs Avro order events | 8087 |
| `schemas` | Shared Avro order schemas and order-status event type | — |

`web/` contains the React/Vite customer interface for authentication, menu search, orders, notifications, administrator analytics, and the Phase-1 assistant endpoint. The Compose stack provides MongoDB, MySQL, ZooKeeper, Kafka, and Schema Registry. Business services register with Eureka except the existing `kafka-consumer`; the registry and Config Server are not clients.

## Databases

The MongoDB database names are configured explicitly:

| Service | MongoDB database |
| --- | --- |
| Authentication | `auth_db` |
| Food catalog / domain | `domain_db` |
| Workflow | `workflow_db` |
| Analytics | `analytics_db` |
| AI Assistant foundation | `ai_db` |
| Notifications | `notification_db` |

Order and inventory persistence use the MySQL databases `order-service` and `inventory-service`, created by `docker/mysql/init/01-create-databases.sql`.

## Configuration and secrets

Copy `.env.example` to `.env` and replace its placeholders. `.env` is ignored by Git. Set a unique random `JWT_SECRET` of at least 32 characters; the same value must be available to Auth Service and API Gateway. No default JWT signing key or MySQL password is supplied. Configure `MONGODB_URI` with the common MongoDB server connection only; each MongoDB service selects its own database from its `application.yml`.

An optional bootstrap administrator can be created by setting both `FOODFUSION_ADMIN_EMAIL` and `FOODFUSION_ADMIN_PASSWORD` for Auth Service. The password must be at least 12 characters.

## Run locally

Set the `.env` values in the PowerShell environment used to launch services. Docker Compose reads `.env` automatically.

After copying and editing `.env`, load its simple `NAME=value` entries into the current PowerShell session before starting Spring Boot or Vite:

```powershell
Get-Content .env | ForEach-Object {
  if ($_ -match '^([A-Za-z_][A-Za-z0-9_]*)=(.*)$') {
    $name = $Matches[1]
    $value = $Matches[2]
    Set-Item -Path "Env:$name" -Value $value
  }
}
```

1. Start the local databases and event infrastructure:

   ```powershell
   docker compose --profile local-db up -d
   ```

2. Start Config Server and then Eureka in separate terminals:

   ```powershell
   .\gradlew.bat :config-server:bootRun
   .\gradlew.bat :discovery-server:bootRun
   ```

   Eureka dashboard: <http://localhost:8761>

3. Start the required application services in separate terminals:

   ```powershell
   .\gradlew.bat :auth-service:bootRun
   .\gradlew.bat :product-service:bootRun
   .\gradlew.bat :inventory-service:bootRun
   .\gradlew.bat :order-service:bootRun
   .\gradlew.bat :workflow-service:bootRun
   .\gradlew.bat :notification-service:bootRun
   .\gradlew.bat :analytics-service:bootRun
   .\gradlew.bat :ai-assistant-service:bootRun
   .\gradlew.bat :api-gateway:bootRun
   ```

   `kafka-consumer` can also be run with `.\gradlew.bat :kafka-consumer:bootRun`. Kafka and Schema Registry use `localhost:9092` and `http://localhost:8086` by default.

4. Run the React UI from `web/`:

   ```powershell
   npm install
   npm run dev
   ```

   The Vite development proxy forwards `/api` requests to the local Gateway. For a production build, run `npm run build`; set `VITE_API_BASE_URL` if the Gateway is not at `http://localhost:8084`.

## Gateway APIs

| Method and path | Access | Purpose |
| --- | --- | --- |
| `POST /api/auth/register` | Public | Create an account |
| `POST /api/auth/login` | Public | Sign in and receive a JWT |
| `GET /api/foods` (also `/api/product`, `/api/food`) | Public | List menu items |
| `GET /api/foods/search?q={query}` | Public | Search the menu |
| `POST /api/foods` (also `/api/product`) | Admin | Add a menu item |
| `POST /api/order` | Authenticated | Place an order |
| `GET /api/order/history` | Authenticated | Get the caller's orders |
| `GET /api/inventory?skuCode={sku}` | Public | Check inventory |
| `GET /api/notifications` | Authenticated | Get the caller's notifications |
| `GET /api/notifications/orders/{orderNumber}` | Owner or admin | Get notifications for one order |
| `GET /api/workflow/orders/{orderNumber}` (also `/api/workflows/orders/{orderNumber}`) | Owner or admin | Get an order workflow |
| `GET /api/analytics/summary` and `/api/analytics/daily` | Admin | Read analytics |
| `POST /api/ai/chat` (also `/api/assistant/chat`) | Authenticated | Search menu items using the assistant foundation |

These paths are served through API Gateway on port 8084. The Gateway validates the token signature and issuer against Auth Service configuration, enforces its route roles, removes client-supplied identity headers, and forwards verified user identity to downstream services.

## Kafka event flow

Order Service publishes Avro `OrderEvent` messages to `order-topic`. Workflow Service and the existing Kafka Consumer consume that topic. Workflow Service persists the order workflow and publishes JSON `OrderStatusEvent` messages to `order-status-topic`; Notification Service and Analytics Service consume those status events and persist their own records. Consumers use separate groups and MongoDB uniqueness constraints for idempotent processing.

## Build and tests

```powershell
.\gradlew.bat build
```

The Product Service integration test uses Testcontainers and needs a running Docker daemon and permission to start MongoDB. To compile and build the modules without that Docker-dependent test:

```powershell
.\gradlew.bat build -x :product-service:test
```

Build the UI with `npm run build` from `web/`. Other service modules currently have no dedicated test sources.

## Phase 2 boundary

Advanced AI work is intentionally deferred. This repository does not implement MongoDB Atlas Search or Vector Search, RAG, AI agents, or AI recommendations. The assistant remains a menu-search foundation; the optional provider adapter is not an advanced retrieval or recommendation system.
