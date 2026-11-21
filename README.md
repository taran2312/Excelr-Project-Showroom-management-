# Showroom Management

A full-stack vehicle showroom management app: inventory across showrooms, customers, test drives, sales with an approval workflow, and staff with role-based access.

| Layer    | Stack |
|----------|-------|
| Backend  | Java 21, Spring Boot 3.5.0 (Web, Security, Data JPA, Validation), JJWT 0.11.5, Lombok, MySQL Connector/J |
| Frontend | React 19, Vite 6, React Router DOM 7, Axios 1.8, Bootstrap 5.3 + plain CSS |
| Database | MySQL 8 |

## Features

- **Auth**: stateless JWT login and registration. New sign-ups get the `SALES` role; the seeded `ADMIN` can promote them.
- **Roles**
  | Action | ADMIN | MANAGER | SALES |
  |---|:-:|:-:|:-:|
  | View everything (dashboard, vehicles, customers, sales, test drives, showrooms) | ✔ | ✔ | ✔ |
  | Create/edit customers, schedule test drives, record sales | ✔ | ✔ | ✔ |
  | Add/edit/delete vehicles | ✔ | ✔ | |
  | Complete or cancel sales, delete customers or test drives | ✔ | ✔ | |
  | View staff list | ✔ | ✔ | |
  | Manage showrooms and staff, delete sales | ✔ | | |
- **Status tracking**
  - Vehicle: `AVAILABLE → RESERVED → SOLD` (driven by sales), or `IN_SERVICE` (set by hand)
  - Sale: `PENDING → COMPLETED | CANCELLED`. Creating a sale reserves the vehicle, completing it marks the vehicle sold, and cancelling releases it.
  - Test drive: `SCHEDULED → COMPLETED | NO_SHOW | CANCELLED`, with feedback
- **Dashboard**: stock counts, revenue from completed sales, upcoming test drives, recent sales.

## Directory tree

```
Project/
├── README.md
├── showroom-management-backend/
│   ├── pom.xml
│   └── src/
│       ├── main/
│       │   ├── java/com/showroom/
│       │   │   ├── ShowroomManagementApplication.java
│       │   │   ├── config/
│       │   │   │   ├── DataSeeder.java                 # seeds admin + first showroom
│       │   │   │   └── security/
│       │   │   │       ├── SecurityConfig.java         # filter chain, CORS, BCrypt, method security
│       │   │   │       ├── JwtUtil.java                # JJWT 0.11.5 create/parse/validate
│       │   │   │       ├── JwtAuthenticationFilter.java
│       │   │   │       ├── JwtAuthEntryPoint.java      # JSON 401s
│       │   │   │       └── CustomUserDetailsService.java
│       │   │   ├── controller/   Auth, User, Showroom, Vehicle, Customer, Sale, TestDrive, Dashboard (+ EnumParser)
│       │   │   ├── dto/          request/response DTOs with Bean Validation
│       │   │   ├── entity/       User, Showroom, Vehicle, Customer, Sale, TestDrive + enums
│       │   │   ├── exception/    GlobalExceptionHandler, ResourceNotFound/BadRequest/Conflict
│       │   │   ├── mapper/       EntityMapper (entity → DTO)
│       │   │   ├── repository/   Spring Data JPA repositories
│       │   │   └── service/      business rules and transactions
│       │   └── resources/application.properties
│       └── test/java/com/showroom/
│           ├── config/security/JwtUtilTest.java                 # token create/validate
│           ├── controller/VehicleControllerSecurityTest.java    # JWT filter, 401/403, validation errors
│           └── service/SaleServiceTest.java, VehicleServiceTest.java  # sale ↔ inventory rules
└── showroom-management-frontend/
    ├── package.json
    ├── vite.config.js            # dev proxy /api → http://localhost:8080
    ├── index.html
    ├── public/favicon.svg
    └── src/
        ├── main.jsx
        ├── App.jsx               # routes
        ├── index.css
        ├── context/              AuthContext.jsx, NotificationContext.jsx
        ├── services/             api.js (axios + JWT interceptor), one service per resource, format.js
        ├── components/           Layout, Navbar, ProtectedRoute, Modal, ConfirmDialog, FormField,
        │                         StatusBadge, StatCard, Toasts, Loader, PageHeader, EmptyState (+ CSS)
        └── pages/                Login, Register, Dashboard, Vehicles, Customers, TestDrives, Sales,
                                  Showrooms, Users, Profile, NotFound (+ Auth.css, Showrooms.css)
```

## Local setup

### Prerequisites

- JDK 21 (`java -version` should report 21)
- Maven 3.9+
- Node.js 18+ and npm
- MySQL 8 running on `localhost:3306`

### 1. MySQL

The JDBC URL uses `createDatabaseIfNotExist=true`, so a user with create rights is enough. To use a dedicated user instead:

```sql
CREATE DATABASE showroom_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'showroom'@'localhost' IDENTIFIED BY 'change-me';
GRANT ALL PRIVILEGES ON showroom_db.* TO 'showroom'@'localhost';
FLUSH PRIVILEGES;
```

Hibernate creates and updates the tables on startup (`ddl-auto=update`).

### 2. Backend (port 8080)

The defaults are in `src/main/resources/application.properties`. Override them with environment variables:

| Variable | Default |
|---|---|
| `DB_URL` | `jdbc:mysql://localhost:3306/showroom_db?createDatabaseIfNotExist=true&...` |
| `DB_USERNAME` / `DB_PASSWORD` | `root` / `root` |
| `JWT_SECRET` | dev-only Base64 key. **Set your own** with `openssl rand -base64 64` |
| `JWT_EXPIRATION_MS` | `86400000` (24h) |
| `CORS_ORIGINS` | `http://localhost:5173` |
| `ADMIN_EMAIL` / `ADMIN_PASSWORD` | `admin@showroom.com` / `Admin@123` (seeded only when the users table is empty) |

```bash
cd showroom-management-backend
DB_USERNAME=showroom DB_PASSWORD=change-me mvn spring-boot:run
```

Run the tests with `mvn test` (23 tests; they need no database), or build a jar with `mvn clean package` and run `java -jar target/showroom-management-backend-0.0.1-SNAPSHOT.jar`.

### 3. Frontend (port 5173)

```bash
cd showroom-management-frontend
npm install
npm run dev
```

Open http://localhost:5173 and sign in as `admin@showroom.com` / `Admin@123`, then change the password under **My profile**.

In development, Vite proxies `/api` to `http://localhost:8080`. For a production build served from another origin, set `VITE_API_URL` (see `.env.example`), run `npm run build`, and add that origin to `CORS_ORIGINS`.

## REST API

All endpoints except `/api/auth/login` and `/api/auth/register` need `Authorization: Bearer <token>`. Errors come back as JSON: `{ status, error, message, path, fieldErrors? }`.

| Method | Path | Notes |
|---|---|---|
| POST | `/api/auth/login` | `{email, password}` → `{token, user, ...}` |
| POST | `/api/auth/register` | `{fullName, email, password, phone?}` |
| GET | `/api/auth/me` | current user |
| PUT | `/api/auth/change-password` | `{currentPassword, newPassword}` |
| GET/POST | `/api/users` | list: ADMIN, MANAGER · create: ADMIN |
| GET/PUT/DELETE | `/api/users/{id}` | write: ADMIN |
| GET/POST | `/api/showrooms` | write: ADMIN |
| GET/PUT/DELETE | `/api/showrooms/{id}` | write: ADMIN |
| GET | `/api/vehicles?status=&showroomId=&q=` | filter and search |
| POST · PUT/DELETE `{id}` | `/api/vehicles` | ADMIN, MANAGER |
| PATCH | `/api/vehicles/{id}/status` | `{status: AVAILABLE \| IN_SERVICE}` |
| GET/POST | `/api/customers?q=` | |
| GET/PUT/DELETE | `/api/customers/{id}` | delete: ADMIN, MANAGER |
| GET/POST | `/api/sales?status=` | create reserves the vehicle |
| GET/PUT/DELETE | `/api/sales/{id}` | edit while PENDING · delete: ADMIN |
| PATCH | `/api/sales/{id}/status` | `{status: COMPLETED \| CANCELLED}` (ADMIN, MANAGER) |
| GET/POST | `/api/test-drives?status=` | |
| GET/PUT/DELETE | `/api/test-drives/{id}` | delete: ADMIN, MANAGER |
| PATCH | `/api/test-drives/{id}/status` | `{status, note?}` |
| GET | `/api/dashboard` | aggregated stats |

Example:

```bash
TOKEN=$(curl -s localhost:8080/api/auth/login -H 'Content-Type: application/json' \
  -d '{"email":"admin@showroom.com","password":"Admin@123"}' | sed -E 's/.*"token":"([^"]+)".*/\1/')
curl -s localhost:8080/api/vehicles -H "Authorization: Bearer $TOKEN"
```
