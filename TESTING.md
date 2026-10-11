# Testing Guide for NovaMart Backend

## Prerequisites

### Docker Requirement
Integration tests use **Testcontainers** to spin up a real PostgreSQL database in Docker containers. This ensures tests run against the same database as production.

**To run integration tests, Docker Desktop must be running:**

1. Install Docker Desktop: https://www.docker.com/products/docker-desktop/
2. Start Docker Desktop (wait for it to fully start)
3. Verify Docker is running: `docker ps`

### Without Docker
If Docker is not available, you have two options:

#### Option 1: Run Unit Tests Only
```bash
mvn test -Dtest="**/unit/**"
```

Unit tests don't require Docker and test business logic in isolation.

#### Option 2: Use H2 In-Memory Database (Fallback)
See the "H2 Fallback Configuration" section below.

## Running Tests

### All Tests (Requires Docker)
```bash
# Set JAVA_HOME (Windows PowerShell)
$env:JAVA_HOME = "C:\Program Files\Java\jdk-17"

# Run all tests
mvn test
```

### Unit Tests Only (No Docker Required)
```bash
mvn test -Dtest="**/unit/**"
```

### Specific Test Class
```bash
mvn test -Dtest=CatalogServiceSortTest
```

### Integration Tests Only (Requires Docker)
```bash
mvn test -Dtest="**/integration/**"
```

## Test Structure

```
src/test/java/commerce/novamart/
├── unit/                          # Unit tests (no database)
│   ├── CatalogServiceSortTest.java
│   ├── CouponDiscountTest.java
│   ├── JwtServiceTest.java
│   └── OrderServiceTotalsTest.java
├── integration/                   # Integration tests (with database)
│   ├── AdminInventoryIntegrationTest.java
│   ├── AdminSalesIntegrationTest.java
│   └── ... (15+ integration tests)
└── support/                       # Test utilities
    ├── IntegrationTestBase.java
    ├── CatalogTestFixtures.java
    └── AuthTestHelper.java
```

## Test Configuration

### application-test.yml
Located at `src/main/resources/application-test.yml`:

```yaml
spring:
  datasource:
    url: jdbc:tc:postgresql:16:///testdb  # Testcontainers JDBC URL
    driver-class-name: org.testcontainers.jdbc.ContainerDatabaseDriver
  jpa:
    hibernate:
      ddl-auto: validate
  flyway:
    enabled: true
```

The `jdbc:tc:postgresql:16:///testdb` URL tells Testcontainers to:
1. Pull PostgreSQL 16 Docker image (if not cached)
2. Start a PostgreSQL container
3. Create a test database
4. Run Flyway migrations
5. Execute tests
6. Stop and remove the container

## H2 Fallback Configuration (Optional)

If Docker is unavailable, create an alternative test profile using H2 in-memory database:

### 1. Add H2 Dependency to pom.xml
```xml
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <scope>test</scope>
</dependency>
```

### 2. Create application-test-h2.yml
```yaml
spring:
  datasource:
    url: jdbc:h2:mem:testdb;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE
    driver-class-name: org.h2.Driver
  jpa:
    hibernate:
      ddl-auto: create-drop
  flyway:
    enabled: false  # Disable Flyway with H2

logging:
  level:
    commerce.novamart: INFO
```

### 3. Run Tests with H2 Profile
```bash
mvn test -Dspring.profiles.active=test-h2
```

**Note:** H2 is not a perfect PostgreSQL replacement. Some PostgreSQL-specific features may not work in H2.

## Troubleshooting

### "Could not find a valid Docker environment"
- **Cause:** Docker Desktop is not running
- **Solution:** Start Docker Desktop and wait for it to fully initialize
- **Verify:** Run `docker ps` - should return a list (even if empty)

### "ApplicationContext failed to load"
- **Cause:** Usually Docker-related or missing test dependencies
- **Check:** Look for "Caused by:" in the error stack trace
- **Common causes:**
  - Docker not running
  - Port conflicts (another container using PostgreSQL port)
  - Insufficient Docker resources

### Tests Pass Locally But Fail in CI
- Ensure CI environment has Docker available
- Use Docker-in-Docker or Testcontainers Cloud for CI
- Alternative: Use H2 profile for CI (less recommended)

### Slow First Test Run
- **Normal:** First run downloads PostgreSQL Docker image (~100MB)
- **Subsequent runs:** Much faster as image is cached
- **Speed up:** Pre-pull image: `docker pull postgres:16`

## Best Practices

1. **Run unit tests frequently** - they're fast and don't need Docker
2. **Run integration tests before commits** - ensures database compatibility
3. **Keep Docker Desktop running** during development if running integration tests
4. **Use transactions** - `@Transactional` on test classes ensures database cleanup between tests
5. **Isolate test data** - Use unique IDs/names (e.g., with timestamps) to avoid conflicts

## Test Coverage

- **Unit Tests:** 4 tests - Pure logic, no database
- **Integration Tests:** 15+ tests - Full Spring Boot context with PostgreSQL
- **Total:** 26 tests

Run with coverage:
```bash
mvn test jacoco:report
```
View report at: `target/site/jacoco/index.html`
