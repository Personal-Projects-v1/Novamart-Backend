# NovaMart Backend - Quick Start

## Project Status: ✅ COMPILATION SUCCESS

The backend compiles successfully and unit tests pass. Integration tests require Docker.

## Quick Commands

### Compile Project
```powershell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-17"
mvn clean compile
```

### Run Unit Tests (No Docker Required)
```powershell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-17"
mvn test -Dtest="**/unit/**"
```
**Result:** ✅ 11/11 tests pass

### Run All Tests (Docker Required)
```powershell
# Start Docker Desktop first, then:
$env:JAVA_HOME = "C:\Program Files\Java\jdk-17"
mvn test
```

### Run Application
```powershell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-17"
mvn spring-boot:run
```

## Project Structure

```
novamart/
├── src/main/java/commerce/novamart/
│   ├── admin/          # Admin operations (inventory, sales)
│   ├── auth/           # Authentication & authorization
│   ├── cart/           # Shopping cart & wishlist
│   ├── catalog/        # Products, categories, brands
│   ├── checkout/       # Order processing
│   ├── common/         # DTOs, exceptions, utilities
│   ├── config/         # Security, CORS, OpenAPI
│   ├── coupon/         # Coupon & promotions
│   ├── payment/        # Payment processing
│   ├── review/         # Product reviews
│   ├── shipping/       # Shipping & addresses
│   └── user/           # User management
├── src/main/resources/
│   ├── application.yml              # Main configuration
│   ├── application-test.yml         # Test config (Testcontainers)
│   ├── application-test-h2.yml      # H2 fallback
│   └── db/migration/                # Flyway migrations
├── src/test/java/
│   ├── unit/           # Unit tests (11 tests, no Docker)
│   ├── integration/    # Integration tests (15 tests, needs Docker)
│   └── support/        # Test utilities & fixtures
├── TESTING.md          # Comprehensive testing guide
├── TEST_STATUS.md      # Current test status
└── QUICK_START.md      # This file
```

## Technology Stack

- **Framework:** Spring Boot 4.1.1
- **Language:** Java 17
- **Database:** PostgreSQL (prod), H2 (test fallback)
- **Security:** Spring Security + JWT
- **Build:** Maven 3.9+
- **Testing:** JUnit 5, Testcontainers
- **Migration:** Flyway
- **API Docs:** SpringDoc OpenAPI 3.0

## Recent Changes

### Compilation Issue Fixed ✅
- **Problem:** `sortFor()` method was package-private
- **Solution:** Changed to `public` in `CatalogService.java`
- **Commit:** `test: Make sortFor() public for test access`

### MockMvc Configuration Fixed ✅
- **Problem:** `@AutoConfigureMockMvc` not available in Spring Boot 4.x tests
- **Solution:** Manual MockMvc setup in `IntegrationTestBase`
- **Commit:** `test: Fix MockMvc setup and add comprehensive testing documentation`

### Test Documentation Added ✅
- Created `TESTING.md` with full testing guide
- Created `TEST_STATUS.md` with current status
- Added H2 fallback configuration
- **Commit:** `docs: Add comprehensive test status documentation`

## Common Issues & Solutions

### Issue: "sortFor() is not public"
**Status:** ✅ FIXED
**Solution:** Method is now public in `CatalogService.java`

### Issue: "Could not find a valid Docker environment"
**Status:** ⚠️ EXPECTED (Docker not running)
**Solution:** Start Docker Desktop, or run unit tests only: `mvn test -Dtest="**/unit/**"`

### Issue: "AutoConfigureMockMvc cannot be resolved"
**Status:** ✅ FIXED
**Solution:** MockMvc now configured manually in `IntegrationTestBase`

## Next Steps

1. **For Development:**
   - Start Docker Desktop if you need to run integration tests
   - Unit tests work without Docker
   - See `TESTING.md` for detailed testing guide

2. **For Deployment:**
   - Set up PostgreSQL database
   - Configure `application-prod.yml`
   - Set environment variables for secrets (JWT secret, DB credentials)

3. **For CI/CD:**
   - Configure Docker-in-Docker for integration tests
   - Or use H2 profile: `mvn test -Dspring.profiles.active=test-h2`

## Links

- **Testing Guide:** `TESTING.md`
- **Test Status:** `TEST_STATUS.md`
- **API Documentation (when running):** http://localhost:8080/swagger-ui.html

## Git Status

**Branch:** dev
**Recent Commits:**
- ✅ docs: Add comprehensive test status documentation
- ✅ test: Fix MockMvc setup and add comprehensive testing documentation
- ✅ test: Add H2 fallback configuration for tests without Docker
- ✅ test: Add @AutoConfigureMockMvc to IntegrationTestBase
- ✅ test: Make sortFor() public for test access

**Status:** Ready for development ✅
