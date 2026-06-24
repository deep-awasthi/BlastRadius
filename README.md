# ⚡ BlastRadius

> **"Ask your codebase anything."**

BlastRadius is an open-source **Engineering Dependency Intelligence Platform** that scans software repositories and automatically discovers relationships between APIs, services, entities, database tables, repositories, scheduled jobs, configurations, events, and source code components.

**Stop guessing. Know your blast radius before you ship.**

---

## Features

| Feature | Description |
|---|---|
| 🔍 **Repository Scanner** | AST-based Java source analysis using **JavaParser** |
| 🕸️ **Dependency Graph** | Interactive visualization with **Cytoscape.js** + **JGraphT** |
| ⚡ **Impact Analysis** | "What breaks if I change X?" — transitive traversal |
| 🔄 **Circular Dependency Detection** | JGraphT `CycleDetector` identifies all cycles |
| 🏗️ **Architecture Drift** | Detects layer violations (Controller → Repository, etc.) |
| 💀 **Dead Code Detection** | Identifies unreferenced components |
| 📊 **Risk Scoring** | 0-100 score based on fan-in, fan-out, and cycle participation |
| 📊 **Git Intelligence** | Bus factor, contributor ownership, co-change pairs |
| 🔐 **JWT Auth + RBAC** | Register, login, role-based access (ADMIN/USER) |
| 📤 **Multi-format Export** | JSON, CSV, PDF reports |
| 🚀 **Docker Compose** | One-command deployment |

---

## Architecture Diagram

```
┌────────────────────────────────────────────────────────────────┐
│                        BlastRadius Platform                     │
│                                                                │
│  ┌─────────────┐     ┌───────────────┐     ┌───────────────┐  │
│  │  Thymeleaf  │     │  REST APIs    │     │  Swagger UI   │  │
│  │  (UI Pages) │     │  (JSON/CSV)   │     │  /swagger-ui  │  │
│  └──────┬──────┘     └───────┬───────┘     └───────────────┘  │
│         │                   │                                  │
│  ┌──────▼───────────────────▼──────────────────────────────┐  │
│  │                    Controller Layer                       │  │
│  │  Auth | Scan | Discovery | Report | Dashboard | Export   │  │
│  └──────────────────────────┬───────────────────────────────┘  │
│                             │                                  │
│  ┌──────────────────────────▼───────────────────────────────┐  │
│  │                     Service Layer                         │  │
│  │  AuthService | ScanService | DependencyGraphService      │  │
│  │  DashboardService | ReportService | GitHistoryAnalyzer   │  │
│  └──────┬──────────────────┬───────────────────────────────┘  │
│         │                  │                                   │
│  ┌──────▼──────┐   ┌───────▼──────────────────────────────┐  │
│  │  JavaParser │   │         Analysis Engines               │  │
│  │  Scanner    │   │  RiskAnalyzer | ArchDriftDetector     │  │
│  │  (AST)      │   │  DeadCodeDetector | GraphManager      │  │
│  └─────────────┘   └────────────────┬─────────────────────┘  │
│                                     │                          │
│  ┌──────────────────────────────────▼───────────────────────┐ │
│  │                     Data Layer                             │ │
│  │   JPA Repositories  │  PostgreSQL  │  Redis Cache         │ │
│  └────────────────────────────────────────────────────────┘ │
└────────────────────────────────────────────────────────────────┘
```

---

## Dependency Graph Model

```
[Controller / API]
      │ CALLS
      ▼
[Service]  ─── DEPENDS_ON ───► [Configuration]
      │ USES
      ▼
[Repository]
      │ DEPENDS_ON
      ▼
[Entity] ──── OWNS ────► [Table]
      │
      ▼
[Event] ◄──── PRODUCES ── [Service]
        ──── CONSUMES ──► [Event Consumer]

[Job] ──── CALLS ────► [Service]
```

---

## Folder Structure

```
BlastRadius/
├── src/
│   ├── main/java/com/blastradius/
│   │   ├── BlastRadiusApplication.java
│   │   ├── analysis/
│   │   │   ├── ArchitectureDriftDetector.java
│   │   │   ├── DeadCodeDetector.java
│   │   │   └── RiskAnalyzer.java
│   │   ├── config/
│   │   │   ├── CacheConfig.java
│   │   │   ├── OpenApiConfig.java
│   │   │   └── SecurityConfig.java
│   │   ├── controller/
│   │   │   ├── AuthController.java
│   │   │   ├── DashboardController.java
│   │   │   ├── DiscoveryController.java
│   │   │   ├── ExportController.java
│   │   │   ├── GitController.java
│   │   │   ├── ReportController.java
│   │   │   ├── ScanController.java
│   │   │   └── ViewController.java
│   │   ├── dto/
│   │   ├── entity/
│   │   ├── exception/
│   │   ├── git/
│   │   │   └── GitHistoryAnalyzer.java
│   │   ├── graph/
│   │   │   └── GraphManager.java          ← JGraphT
│   │   ├── repository/
│   │   ├── scanner/
│   │   │   └── JavaSourceScanner.java     ← JavaParser
│   │   ├── security/
│   │   └── service/
│   ├── main/resources/
│   │   ├── application.yml
│   │   ├── static/css/main.css
│   │   └── templates/
│   │       ├── dashboard.html
│   │       ├── graph.html
│   │       ├── login.html
│   │       ├── register.html
│   │       ├── reports.html
│   │       └── scanner.html
│   └── test/
├── Dockerfile
├── docker-compose.yml
├── pom.xml
├── requests.http
├── run.sh
└── stop.sh
```

---

## Local Setup

### Prerequisites

- Java 21+
- Maven 3.9+
- Docker & Docker Compose

### Quick Start

```bash
# Clone the repository
git clone <repo-url>
cd BlastRadius

# Start everything with one command
./run.sh
```

This will:
1. Build the JAR with Maven
2. Build the Docker image
3. Start PostgreSQL, Redis, and the app
4. Poll until the app is ready

---

## Docker Setup

```bash
# Start
./run.sh

# Stop
./stop.sh

# View logs
docker compose logs -f blast-radius-app

# Rebuild without cache
docker compose build --no-cache
docker compose up -d
```

### Services

| Service | Port | Description |
|---|---|---|
| `blast-radius-app` | 8080 | Spring Boot Application |
| `postgres` | 5432 | PostgreSQL 16 Database |
| `redis` | 6379 | Redis 7 Cache |

---

## API Documentation

| URL | Description |
|---|---|
| `http://localhost:8080` | Dashboard UI |
| `http://localhost:8080/swagger-ui.html` | Swagger REST API docs |
| `http://localhost:8080/api-docs` | OpenAPI JSON spec |
| `http://localhost:8080/actuator/health` | Health check |

### Key API Endpoints

```
POST /api/auth/register         Register user
POST /api/auth/login            Login → JWT tokens
POST /api/repositories/scan     Trigger scan
GET  /api/apis?scanId=1         Discovered APIs
GET  /api/services?scanId=1     Discovered services
GET  /api/entities?scanId=1     JPA entities
GET  /api/tables?scanId=1       Database tables
GET  /api/dependencies?scanId=1 All relationships
GET  /api/impact-analysis       Blast radius for component
GET  /api/risk?scanId=1         Risk-ranked components
GET  /api/circular-dependencies Cycle detection
GET  /api/dead-code?scanId=1    Unused components
GET  /api/reports/risk          Risk report
GET  /api/reports/architecture  Architecture violations
GET  /api/dashboard?scanId=1    Aggregated metrics
GET  /api/export/json           Graph JSON export
GET  /api/export/csv            Components CSV export
GET  /api/export/pdf/risk       Risk PDF report
GET  /api/git/analyze           Git intelligence
```

---

## Security

- **JWT Authentication** (`io.jsonwebtoken` / JJWT 0.12)
- **BCrypt** password hashing
- **Role-Based Access Control**: `ADMIN` and `USER` roles
- Stateless session (no cookies)
- All `/api/**` endpoints require `Authorization: Bearer <token>` header

---

## Database Schema

```sql
-- Users
CREATE TABLE users (
    id          BIGSERIAL PRIMARY KEY,
    username    VARCHAR(50)  UNIQUE NOT NULL,
    password    TEXT         NOT NULL,
    role        VARCHAR(20)  NOT NULL DEFAULT 'USER',
    created_at  TIMESTAMP    NOT NULL,
    updated_at  TIMESTAMP
);

-- Scans
CREATE TABLE scans (
    id                  BIGSERIAL PRIMARY KEY,
    repo_path           VARCHAR(1000) NOT NULL,
    repo_name           VARCHAR(500),
    status              VARCHAR(20)   NOT NULL,
    scanned_at          TIMESTAMP,
    completed_at        TIMESTAMP,
    total_files         INT DEFAULT 0,
    total_components    INT DEFAULT 0,
    total_relationships INT DEFAULT 0,
    error_message       VARCHAR(2000),
    created_at          TIMESTAMP NOT NULL
);

-- Component Nodes (graph vertices)
CREATE TABLE component_nodes (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(500)  NOT NULL,
    qualified_name  VARCHAR(1000),
    component_type  VARCHAR(30)   NOT NULL,
    file_path       VARCHAR(1000),
    package_name    VARCHAR(500),
    lines_of_code   INT,
    http_method     VARCHAR(10),
    endpoint_path   VARCHAR(500),
    table_name      VARCHAR(200),
    metadata        TEXT,
    risk_score      DOUBLE PRECISION DEFAULT 0,
    risk_category   VARCHAR(20)   DEFAULT 'LOW',
    is_dead_code    BOOLEAN       DEFAULT FALSE,
    scan_id         BIGINT REFERENCES scans(id),
    created_at      TIMESTAMP NOT NULL
);

-- Component Relationships (graph edges)
CREATE TABLE component_relationships (
    id                BIGSERIAL PRIMARY KEY,
    source_node_id    BIGINT REFERENCES component_nodes(id),
    target_node_id    BIGINT REFERENCES component_nodes(id),
    relationship_type VARCHAR(30),
    description       VARCHAR(500),
    scan_id           BIGINT REFERENCES scans(id),
    created_at        TIMESTAMP NOT NULL
);
```

---

## Technology Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.2 |
| AST Parsing | **JavaParser 3.26** |
| Graph Engine | **JGraphT 1.5** |
| Database | PostgreSQL 16 |
| Cache | Redis 7 + Caffeine |
| Security | Spring Security + JJWT |
| UI | Thymeleaf + Chart.js + Cytoscape.js |
| API Docs | SpringDoc OpenAPI / Swagger |
| Monitoring | Spring Boot Actuator |
| Container | Docker + Docker Compose |
| PDF Export | OpenPDF |
| Testing | JUnit 5 + Mockito + MockMvc |

---

## Troubleshooting

**App fails to start with DB connection error:**
```bash
# Check PostgreSQL is healthy
docker compose ps
docker compose logs postgres
```

**Redis connection refused:**
```bash
docker compose logs redis
```

**Scan fails: "Repository path does not exist":**
- When running in Docker, the host filesystem is mounted at `/host`. Pass `/host/path/to/repo` instead.
- Or use the local Maven run (`mvn spring-boot:run`) for direct host access.

**Run locally without Docker:**
```bash
# Start only DB and Redis with Docker
docker compose up -d postgres redis

# Run the app locally (uses host repo paths directly)
mvn spring-boot:run
```

**Port 8080 already in use:**
```bash
lsof -ti:8080 | xargs kill -9
```

---

## Future Enhancements

- [ ] Kotlin / Gradle project support
- [ ] Multi-module Maven project analysis
- [ ] GitHub / GitLab API integration (remote scan)
- [ ] Slack / Teams notifications on high-risk changes
- [ ] CI/CD pipeline integration plugin
- [ ] Neo4j graph database backend option
- [ ] GraphQL query interface
- [ ] VS Code extension for in-editor impact analysis

---

*Built with ⚡ by BlastRadius — MIT License*
