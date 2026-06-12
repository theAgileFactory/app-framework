# BizDock Migration Guide: Java 8 → 21, Play 2.4 → 3.0, Docker Modernization

**Total Estimated Duration: 60-70 days**

---

## Executive Summary

This guide provides a detailed, phase-by-phase migration path for modernizing the BizDock ecosystem from:
- **Java 1.8** → **Java 21 LTS**
- **Play Framework 2.4.2** → **Play Framework 3.0.x LTS**
- **Scala 2.11** → **Scala 2.13**
- **CentOS 7** → **Ubuntu 22.04 LTS**
- **Maven Play2 Plugin** → **Gradle or native SBT**

### Current Architecture Issues
- Play 2.4 reached End-of-Life in 2017 (9 years without updates)
- Java 8 security patches ended in December 2023
- Multiple deprecated dependencies (Ebean Avaje ORM, Wordnik Swagger 1.3)
- CentOS 7 support ended June 30, 2024
- MySQL Connector 5.1 is obsolete

---

## Phase 1: Assessment & Preparation (5 days)

### Day 1-2: Codebase Analysis

**Objectives:**
- Document current dependency versions
- Identify deprecated APIs
- List custom code patterns

**Tasks:**

1. **Generate Dependency Report**
   ```bash
   cd app-framework
   mvn dependency:tree > dependencies_current.txt
   mvn versions:display-updates > available_updates.txt
   ```

2. **Search for Deprecated Patterns** (based on findings)
   - `javax.*` imports → need migration to `jakarta.*`
   - `com.avaje.ebean.*` → needs replacement with EBean 13.x or Hibernate
   - `com.wordnik.swagger.*` → migrate to Swagger 2.0/OpenAPI 3.0
   - `play.Play.application()` → replace with dependency injection
   - Direct model queries using `.find` → may need refactoring

3. **Document Key Findings**
   ```
   From codebase analysis, found:
   - javax.inject (found in app-framework)
   - javax.persistence (found in all model files)
   - javax.ws.rs (found in controllers)
   - javax.mail (found in EmailServiceImpl)
   - com.avaje.ebean (used extensively in models)
   - com.wordnik.swagger (used in API controllers)
   - play.Play.application() (used in multiple places)
   ```

4. **Create Migration Checklist**
   - [ ] All javax.* to jakarta.* replacements
   - [ ] EBean queries updated
   - [ ] Swagger annotations updated
   - [ ] Configuration files migrated
   - [ ] Build files converted to Gradle
   - [ ] Docker base images updated
   - [ ] Tests passing in new environment

**Day 1-2 Deliverables:**
- `CURRENT_STATE_ANALYSIS.md` (comprehensive dependency analysis)
- `DEPRECATED_APIS.md` (list of all deprecated patterns found)
- `MIGRATION_CHECKLIST.md` (tracked during entire migration)

---

### Day 3: Environment Setup

**Objectives:**
- Set up parallel build environments
- Create migration branches
- Install required tools

**Tasks:**

1. **Create Git Branches**
   ```bash
   # For each repository
   git checkout -b migration/java21-play3.0
   git push -u origin migration/java21-play3.0
   ```

2. **Install Build Tools**
   ```bash
   # Java 21
   # Linux/Mac: use sdkman
   curl -s "https://get.sdkman.io" | bash
   sdk install java 21.0.1-oracle
   sdk install gradle 8.5
   
   # Or download from oracle.com
   # For Windows: use Chocolatey or direct download
   ```

3. **Create Build Matrix Script** (`build_matrix.sh`)
   ```bash
   #!/bin/bash
   # Test building with multiple Java versions
   
   echo "Building with Java 8..."
   sdk use java 8.0.392-zulu
   mvn clean install -DskipTests
   
   echo "Building with Java 11..."
   sdk use java 11.0.20-oracle
   mvn clean install -DskipTests
   
   echo "Building with Java 21..."
   sdk use java 21.0.1-oracle
   mvn clean install -DskipTests
   ```

4. **Set Up IDE Configuration**
   - Eclipse: Update Project Facets to Java 21
   - IntelliJ: Set Language Level to 21 in Project Structure
   - VS Code: Update `.vscode/settings.json`

**Day 3 Deliverables:**
- Migration branches created in all repos
- Build environment ready for Java 21
- Build matrix test script

---

### Day 4-5: Database and Dependency Analysis

**Objectives:**
- Audit database schema compatibility
- Identify dependency conflicts
- Plan upgrade order

**Tasks:**

1. **MySQL Connector Analysis**
   - Current: 5.1.36 (2011, JDBC 4.0)
   - Target: 8.0.x (2018, JDBC 4.3, supports Java 21)
   - Action: Check connection properties compatibility

   ```java
   // Current MySQL 5.1.36 properties
   com.mysql.jdbc.Driver → com.mysql.cj.jdbc.Driver (new in 8.0)
   
   // Check application.conf for any legacy driver settings
   ```

2. **Create Upgrade Priority Matrix**
   | Component | Difficulty | Priority | Current | Target |
   |-----------|-----------|----------|---------|--------|
   | Java | Low | High | 8 | 21 |
   | MySQL Connector | Low | High | 5.1 | 8.0 |
   | Scala | Medium | Medium | 2.11 | 2.13 |
   | Play Framework | High | High | 2.4.2 | 3.0.x |
   | Ebean ORM | High | High | Avaje | 13.x |
   | Swagger | Medium | Medium | 1.3 | 2.0/3.0 |

3. **Test Database with New Connector**
   - Set up test environment with MySQL 8.0 driver
   - Run basic connectivity tests
   - Verify prepared statements work
   - Check for connection pool compatibility

**Day 4-5 Deliverables:**
- `DEPENDENCY_AUDIT.md`
- `UPGRADE_PRIORITY.md`
- Database compatibility test results

---

## Phase 2: Intermediate Upgrades (8 days)

### Day 6: Java 8 → Java 11

**Objectives:**
- Upgrade to Java 11 (intermediate LTS)
- Identify Java 9+ API changes
- Update build configuration

**Tasks:**

1. **Update pom.xml in all modules**
   ```xml
   <!-- OLD -->
   <java.source>1.8</java.source>
   <java.target>1.8</java.target>
   <maven-compiler-plugin.version>2.3.1</maven-compiler-plugin.version>
   
   <!-- NEW -->
   <java.source>11</java.source>
   <java.target>11</java.target>
   <maven-compiler-plugin.version>3.11.0</maven-compiler-plugin.version>
   
   <!-- Add release property for Java 9+ modules -->
   <maven.compiler.release>11</maven.compiler.release>
   ```

2. **Update Maven Compiler Plugin Configuration**
   ```xml
   <plugin>
       <groupId>org.apache.maven.plugins</groupId>
       <artifactId>maven-compiler-plugin</artifactId>
       <version>3.11.0</version>
       <configuration>
           <release>11</release>
           <compilerArgs>
               <arg>-Xlint:deprecation</arg>
           </compilerArgs>
       </configuration>
   </plugin>
   ```

3. **Test Compilation**
   ```bash
   cd app-framework
   mvn clean compile -Dmaven.compiler.release=11
   # Watch for deprecation warnings about javax.* packages
   ```

4. **Identify Module System Issues**
   - Check for uses of internal JDK classes (sun.*, com.sun.*)
   - Verify classpath dependencies (no modules affected yet)
   - Record any `"unnamed module"` errors

**Day 6 Deliverables:**
- Updated pom.xml files with Java 11 settings
- Compilation successful with 0 errors (warnings OK)
- List of deprecated APIs to address

---

### Day 7: Update MySQL Connector & Legacy Dependencies

**Objectives:**
- Upgrade MySQL Connector
- Update deprecated libraries
- Maintain API compatibility where possible

**Tasks:**

1. **Update MySQL Connector (all pom.xml files)**
   ```xml
   <!-- OLD -->
   <mysql-connector-java.version>5.1.36</mysql-connector-java.version>
   <dependency>
       <groupId>mysql</groupId>
       <artifactId>mysql-connector-java</artifactId>
       <version>5.1.36</version>
   </dependency>
   
   <!-- NEW -->
   <mysql-connector-java.version>8.0.33</mysql-connector-java.version>
   <dependency>
       <groupId>com.mysql</groupId>
       <artifactId>mysql-connector-j</artifactId>
       <version>8.0.33</version>
   </dependency>
   ```

2. **Update Configuration Files**
   ```conf
   # OLD (application.conf)
   db.default.driver=com.mysql.jdbc.Driver
   
   # NEW
   db.default.driver=com.mysql.cj.jdbc.Driver
   
   # Add modern JDBC parameters
   db.default.url="jdbc:mysql://localhost:3306/bizdock?useSSL=false&serverTimezone=UTC"
   ```

3. **Update Other Outdated Dependencies**
   ```xml
   <!-- POI 3.12 → 5.2.3 -->
   <poi.version>5.2.3</poi.version>
   
   <!-- JasperReports 6.4.1 → 6.20.5 -->
   <jasperreports.version>6.20.5</jasperreports.version>
   
   <!-- Commons dependencies -->
   <commons-io.version>2.13.0</commons-io.version>
   <commons-lang3.version>3.13.0</commons-lang3.version>
   <commons-codec.version>1.16.0</commons-codec.version>
   
   <!-- Maven plugins -->
   <maven-resources-plugin.version>3.3.1</maven-resources-plugin.version>
   <maven-antrun-plugin.version>3.1.0</maven-antrun-plugin.version>
   ```

4. **Test Builds with New Dependencies**
   ```bash
   mvn clean compile
   # Expected: May see deprecation warnings about pac4j versions
   ```

5. **Update Database Connection Tests**
   - Create test class to verify new MySQL connector
   - Verify connection pooling works
   - Test prepared statements

**Day 7 Deliverables:**
- Updated MySQL Connector 8.0
- All legacy dependencies updated
- Build successful with new versions

---

### Day 8: Migrate javax.* → jakarta.*

**Objectives:**
- Replace all deprecated javax packages with jakarta equivalents
- Update security and persistence APIs
- Maintain Play Framework 2.4 (temporary)

**Tasks:**

1. **Create Automated Migration Script** (`migrate_javax_to_jakarta.sh`)
   ```bash
   #!/bin/bash
   # Migrate javax to jakarta in all Java files
   
   for file in $(find . -name "*.java" -type f); do
       # Basic replacements
       sed -i 's/import javax\.inject\./import jakarta.inject./g' "$file"
       sed -i 's/import javax\.persistence\./import jakarta.persistence./g' "$file"
       sed -i 's/import javax\.mail\./import jakarta.mail./g' "$file"
       sed -i 's/import javax\.ws\.rs\./import jakarta.ws.rs./g' "$file"
       sed -i 's/import javax\.servlet\./import jakarta.servlet./g' "$file"
   done
   ```

2. **Manual Audit and Replacement** (critical imports)
   - Review each replacement for correctness
   - Handle special cases where needed

   **Files requiring attention:**
   - `app/framework/services/email/EmailServiceImpl.java`: javax.mail → jakarta.mail
   - `app/framework/utils/Msg.java`: javax.inject → jakarta.inject
   - All model files in `app/models/`: javax.persistence → jakarta.persistence
   - All API controllers: javax.ws.rs → jakarta.ws.rs

3. **Update Dependency POM**
   ```xml
   <!-- Add Jakarta dependencies -->
   <dependency>
       <groupId>jakarta.inject</groupId>
       <artifactId>jakarta.inject-api</artifactId>
       <version>2.0.1</version>
   </dependency>
   
   <dependency>
       <groupId>jakarta.persistence</groupId>
       <artifactId>jakarta.persistence-api</artifactId>
       <version>3.1.0</version>
   </dependency>
   
   <dependency>
       <groupId>jakarta.ws.rs</groupId>
       <artifactId>jakarta.ws.rs-api</artifactId>
       <version>3.1.0</version>
   </dependency>
   
   <dependency>
       <groupId>com.sun.mail</groupId>
       <artifactId>jakarta.mail</artifactId>
       <version>2.0.1</version>
   </dependency>
   ```

4. **Test Compilation**
   ```bash
   mvn clean compile
   # Should have 0 errors related to javax packages
   ```

**Day 8 Deliverables:**
- All javax.* → jakarta.* migrations complete
- Successful compilation with new packages
- Test suite passing (or noted for later)

---

### Day 9: PAC4j & Security Updates

**Objectives:**
- Update authentication/authorization frameworks
- Test with new versions
- Document breaking changes

**Tasks:**

1. **Update PAC4j Version**
   ```xml
   <!-- OLD -->
   <pac4j-http.version>1.7.0</pac4j-http.version>
   <pac4j-cas.version>1.7.0</pac4j-cas.version>
   <pac4j-core.version>1.7.0</pac4j-core.version>
   <pac4j-saml.version>1.7.0</pac4j-saml.version>
   <play-pac4j-java.version>1.5.0</play-pac4j-java.version>
   
   <!-- NEW -->
   <pac4j-http.version>5.7.0</pac4j-http.version>
   <pac4j-cas.version>5.7.0</pac4j-cas.version>
   <pac4j-core.version>5.7.0</pac4j-core.version>
   <pac4j-saml.version>5.7.0</pac4j-saml.version>
   <play-pac4j-java.version>11.1.0</play-pac4j-java.version>
   ```

2. **Update Deadbolt Version**
   ```xml
   <!-- For Play 2.4 compatibility (temporary) -->
   <deadbolt.version>2.8.3</deadbolt.version>
   
   <!-- Note: Will need major update for Play 3.0 -->
   ```

3. **Test Authentication Flows**
   - Test SSO authentication
   - Test SAML configuration
   - Test CAS integration
   - Document any API changes

4. **Update Configuration**
   - Review `framework.conf` for security settings
   - Update any hardcoded security policies
   - Test with test users

**Day 9 Deliverables:**
- PAC4j updated to version 5.7.0
- Authentication tests passing
- Security configuration validated

---

### Day 10: Scala & Play 2.4 Build System

**Objectives:**
- Update Scala to 2.13
- Validate Play 2.4 compatibility
- Prepare for Play 3.0 migration

**Tasks:**

1. **Update build.sbt Files**
   ```scala
   // OLD (in all build.sbt files)
   scalaVersion := "2.11.6"
   
   // NEW
   scalaVersion := "2.13.12"
   ```

2. **Update Maven Play2 Plugin**
   ```xml
   <!-- OLD -->
   <play2-maven-plugin.version>1.2.5</play2-maven-plugin.version>
   
   <!-- NEW - This plugin is obsolete, we'll move to Gradle -->
   <!-- For now, update to latest available -->
   <play2-maven-plugin.version>1.3.5</play2-maven-plugin.version>
   ```

3. **Compile with New Scala**
   ```bash
   mvn clean compile
   # May see Scala 2.13 deprecation warnings
   ```

4. **Fix Scala Compatibility Issues**
   - Scala 2.13 removed many deprecated features
   - Review compiler warnings
   - Update any Scala-specific code

5. **Test with SBT Directly** (prepare for Play 3.0)
   ```bash
   cd maf-desktop-app
   sbt clean compile
   sbt test
   ```

**Day 10 Deliverables:**
- Scala updated to 2.13.12
- Compilation successful with SBT
- Play 2.4 still working
- Ready for Play 3.0 migration

---

## Phase 3: Play Framework Migration (20 days)

### Day 11-12: Play Framework 2.4 → 2.8 (Intermediate)

**Objectives:**
- Move to latest Play 2.x before jumping to 3.0
- Update routing configuration
- Test compatibility

**Tasks:**

1. **Update Play Version in POM** (all modules)
   ```xml
   <!-- OLD -->
   <play.version>2.4.2</play.version>
   
   <!-- NEW (still 2.x, but much newer) -->
   <play.version>2.8.18</play.version>
   ```

2. **Update Play Dependencies**
   ```xml
   <!-- Update all Play artifacts to 2.8.18 -->
   <dependency>
       <groupId>com.typesafe.play</groupId>
       <artifactId>play_2.13</artifactId>
       <version>2.8.18</version>
   </dependency>
   
   <dependency>
       <groupId>com.typesafe.play</groupId>
       <artifactId>play-java_2.13</artifactId>
       <version>2.8.18</version>
   </dependency>
   
   <!-- Note: Now using Scala 2.13 -->
   ```

3. **Update Routes Configuration**
   - Play 2.8 has updated routing syntax
   - Review `conf/routes` files
   - No major breaking changes expected

4. **Test Compilation and Runtime**
   ```bash
   mvn clean install -DskipTests
   
   # In Docker/Test Environment
   docker run -it bizdock:test ./opt/prepare/build.sh
   ```

5. **Update Play Configuration**
   ```conf
   # application.conf - Update Play settings
   play.i18n.langs=["en","fr","de"]
   
   # Enable new Play 2.8 features if needed
   play.server.netty.eventLoopThreads=4
   ```

**Day 11-12 Deliverables:**
- Play Framework updated to 2.8.18
- All compilation successful
- Routes working correctly
- Ready for Play 3.0 migration

---

### Day 13-14: Plan Play 3.0 Migration

**Objectives:**
- Document all breaking changes
- Create migration strategy
- Prepare codebase for major changes

**Tasks:**

1. **Create Play 2.8 → 3.0 Breaking Changes Document**

   **Major Changes:**
   - Package names: `play.*` mostly same, but module reorganization
   - Configuration format: Still HOCON but some property names changed
   - Router: Updated DSL, some methods renamed
   - Database: Ebean removed, use JPA only or ORM replacement
   - Testing: Updated testing utilities
   - HTTP: Some deprecated methods removed
   - Forms: Updated validation APIs

2. **Audit Key Code Areas for Play 3.0 Compatibility**

   ```java
   // ISSUE 1: play.Play.application() - REMOVED in Play 3.0
   // CURRENT CODE FOUND IN: framework/utils/Pagination.java
   Play.application().configuration().getInt("maf.number_page_links")
   
   // SOLUTION: Use dependency injection
   @Inject
   private Configuration configuration;
   // Then: configuration.getInt("maf.number_page_links")
   
   
   // ISSUE 2: Ebean ORM - REMOVED from Play 3.0
   // CURRENT CODE: Used in all model files (com.avaje.ebean.Model)
   import com.avaje.ebean.Model;
   public class Actor extends Model { ... }
   
   // SOLUTION: Migrate to JPA-based Ebean 13.x or use Hibernate
   
   
   // ISSUE 3: play.libs.F.Promise - DEPRECATED
   // CURRENT CODE: Multiple controller methods
   public Promise<Result> display(Long widgetId) { ... }
   
   // SOLUTION: Replace with CompletableFuture or play.mvc.Result
   
   
   // ISSUE 4: Wordnik Swagger - REMOVED
   // CURRENT: import com.wordnik.swagger.annotations.*
   
   // SOLUTION: Use Swagger 2.0 or migrate to Spring Boot (alternative)
   ```

3. **Categorize Files by Difficulty**

   | Category | Difficulty | Files | Estimated Effort |
   |----------|-----------|-------|-----------------|
   | Models (JPA migration) | High | 50+ | 15 days |
   | Controllers (DI + routing) | High | 30+ | 12 days |
   | Services (Promise → CF) | Medium | 40+ | 8 days |
   | Configuration | Low | 5 | 1 day |
   | Tests | Medium | 30+ | 5 days |

4. **Create Migration Branch Strategy**
   ```bash
   # Main migration branch
   git checkout -b migration/play3.0
   
   # Feature branches for each module
   git checkout -b migration/play3.0/models
   git checkout -b migration/play3.0/controllers
   git checkout -b migration/play3.0/services
   ```

5. **Document Ebean → JPA Decision**
   
   **Option A: Ebean 13.x (Recommended)**
   - Minimal changes to existing model code
   - JPA-compatible
   - Good Play 3.0 support
   - Already using Ebean queries
   
   **Option B: Hibernate + JPA**
   - More standard ORM
   - Larger code changes
   - Better ecosystem
   
   **Decision: Go with Ebean 13.x for minimal disruption**

**Day 13-14 Deliverables:**
- Comprehensive Play 2.8 → 3.0 migration guide
- Categorized breaking changes
- Ebean strategy decided
- Migration branch structure ready

---

### Day 15-19: Ebean ORM Migration (Core)

**Objectives:**
- Migrate from Avaje Ebean 4.x to Ebean 13.x
- Update all model definitions
- Maintain database compatibility

**Tasks:**

1. **Update Ebean Dependency**
   ```xml
   <!-- Remove old Ebean Avaje -->
   <!-- <dependency>
        <groupId>org.avaje.ebeanorm</groupId>
        <artifactId>avaje-ebeanorm</artifactId>
        <version>4.6.2</version>
      </dependency> -->
   
   <!-- Add new Ebean -->
   <dependency>
       <groupId>io.ebean</groupId>
       <artifactId>ebean</artifactId>
       <version>13.19.1</version>
   </dependency>
   
   <dependency>
       <groupId>io.ebean</groupId>
       <artifactId>ebean-ddl-generator</artifactId>
       <version>13.19.1</version>
       <scope>test</scope>
   </dependency>
   ```

2. **Update Model Base Class**
   
   **OLD (Avaje Ebean)**
   ```java
   import com.avaje.ebean.Model;
   
   @Entity
   public class Actor extends Model {
       @Id
       public Long id;
       public String name;
   }
   ```
   
   **NEW (Ebean 13)**
   ```java
   import io.ebean.Model;
   
   @Entity
   public class Actor extends Model {
       @Id
       public Long id;
       public String name;
   }
   ```

3. **Update Ebean Query API**
   
   **OLD**
   ```java
   import com.avaje.ebean.*;
   
   public class ActorDao {
       public static Finder<Long, Actor> findActor = new Finder<>(Actor.class);
       
       public static Actor getActorById(Long id) {
           return findActor.where()
               .eq("deleted", false)
               .eq("id", id)
               .findUnique();
       }
   }
   ```
   
   **NEW**
   ```java
   import io.ebean.*;
   
   public class ActorDao {
       public static Finder<Long, Actor> findActor = new Finder<>(Actor.class);
       
       public static Actor getActorById(Long id) {
           return findActor.query()
               .where()
               .eq("deleted", false)
               .eq("id", id)
               .findOne();
       }
   }
   ```

4. **Update Expression API**
   
   **OLD**
   ```java
   import com.avaje.ebean.Expr;
   import com.avaje.ebean.Expression;
   import com.avaje.ebean.ExpressionList;
   
   ExpressionList<Reporting> query = ReportingDao.findReporting
       .where()
       .raw("custom_expression");
   ```
   
   **NEW**
   ```java
   import io.ebean.*;
   
   Query<Reporting> query = ReportingDao.findReporting.query()
       .where()
       .raw("custom_expression");
   ```

5. **Migrate Configuration**
   ```conf
   # OLD (application.conf)
   ebean.default=["models.*"]
   
   # NEW - Ebean 13 uses automatic scanning
   # Can keep same or be more explicit
   ebean.default=["models.pmo.*", "models.delivery.*", "models.governance.*", "models.reporting.*"]
   ```

6. **Update fetch() Calls**
   
   **OLD**
   ```java
   expressionList = ReportingDao.findReporting
       .fetch("reportingAuthorization.principals", new FetchConfig().lazy())
       .where();
   ```
   
   **NEW**
   ```java
   expressionList = ReportingDao.findReporting.query()
       .fetch("reportingAuthorization.principals", new FetchConfig().lazy())
       .where();
   ```

7. **Test All Model Operations**
   ```bash
   # Create test class
   # Test CRUD operations
   # Test queries with new API
   # Test relationships
   
   mvn test -Dtest=*DaoTest
   ```

**Day 15-19 Deliverables:**
- Ebean 13.19.1 dependency added
- All models updated to use new Ebean API
- All DAO files updated for query syntax
- Database queries tested and working

---

### Day 20-22: Swagger API Update

**Objectives:**
- Migrate from Wordnik Swagger 1.3 to Swagger 2.0/OpenAPI 3.0
- Update API annotations
- Test API documentation

**Tasks:**

1. **Choose Swagger Version Strategy**
   
   **Option A: Swagger 2.0 (Stable)**
   - `springfox-swagger2:2.10.5`
   - Gradual migration path
   - Good Play Framework support
   
   **Option B: OpenAPI 3.0 (Modern)**
   - `springdoc-openapi:1.8.0`
   - Better spec compliance
   - Future-proof
   
   **Decision: Use Swagger 2.0 for compatibility, plan OpenAPI 3.0 later**

2. **Update Swagger Dependencies**
   ```xml
   <!-- Remove old Wordnik -->
   <!-- <dependency>
        <groupId>com.wordnik</groupId>
        <artifactId>swagger-jaxrs_${scala.version}</artifactId>
        ...
      </dependency> -->
   
   <!-- Add new Swagger -->
   <dependency>
       <groupId>io.swagger</groupId>
       <artifactId>swagger-jaxrs</artifactId>
       <version>1.6.14</version>
   </dependency>
   
   <dependency>
       <groupId>io.swagger</groupId>
       <artifactId>swagger-core</artifactId>
       <version>1.6.14</version>
   </dependency>
   ```

3. **Update Annotations** (Search and Replace)
   
   **OLD**
   ```java
   import com.wordnik.swagger.annotations.*;
   
   @Api(value = "/api/core/kpi", description = "Operations on KPIs")
   public class KpiApiController {
       @ApiOperation(...)
       @ApiResponses(...)
       public Result addKpiData() { ... }
   }
   ```
   
   **NEW**
   ```java
   import io.swagger.annotations.*;
   
   @Api(value = "/api/core/kpi", description = "Operations on KPIs")
   public class KpiApiController {
       @ApiOperation(...)
       @ApiResponses(...)
       public Result addKpiData() { ... }
   }
   ```

4. **Update Swagger Configuration**
   ```java
   // Create Swagger configuration class
   @Configuration
   public class SwaggerConfig {
       @Bean
       public Docket api() {
           return new Docket(DocumentationType.SWAGGER_2)
               .select()
               .apis(RequestHandlerSelectors.basePackage("controllers.api"))
               .paths(PathSelectors.any())
               .build()
               .apiInfo(apiInfo());
       }
       
       private ApiInfo apiInfo() {
           return new ApiInfo(
               "BizDock API",
               "API for BizDock application",
               "1.0",
               null,
               null,
               null,
               null
           );
       }
   }
   ```

5. **Test API Documentation**
   - Build application
   - Navigate to `/swagger-ui.html`
   - Verify all endpoints documented
   - Check parameter/response schemas

**Day 20-22 Deliverables:**
- Swagger 2.0 integrated
- All API annotations updated
- API documentation accessible

---

### Day 23-25: Controller & Routing Migration

**Objectives:**
- Update controller routing
- Replace play.libs.F.Promise with CompletableFuture
- Update form handling

**Tasks:**

1. **Update Promise Usages**
   
   **OLD (Play 2.x)**
   ```java
   import play.libs.F.Promise;
   import play.libs.F.Function0;
   
   public Promise<Result> display(Long widgetId) {
       return Promise.promise(new Function0<Result>() {
           @Override
           public Result apply() throws Throwable {
               return ok("content");
           }
       });
   }
   ```
   
   **NEW (Java 8+ CompletableFuture)**
   ```java
   import java.util.concurrent.CompletableFuture;
   
   public CompletableFuture<Result> display(Long widgetId) {
       return CompletableFuture.completedFuture(ok("content"));
   }
   
   // Or with async work
   public CompletableFuture<Result> display(Long widgetId) {
       return CompletableFuture.supplyAsync(() -> {
           // Async work here
           return ok("content");
       });
   }
   ```

2. **Replace play.Play.application()**
   
   **OLD**
   ```java
   import play.Play;
   
   Play.application().configuration().getInt("maf.number_page_links")
   ```
   
   **NEW (Dependency Injection)**
   ```java
   import play.Configuration;
   import javax.inject.Inject;
   
   @Inject
   private Configuration configuration;
   
   configuration.getInt("maf.number_page_links")
   ```

3. **Update Router Declarations**
   
   Review `conf/routes` for Play 2.8 compatibility (mostly compatible, check for any warnings)

4. **Update Form Handling**
   
   **OLD**
   ```java
   Form<KpiDataRequest> kpiDataRequestForm = 
       kpiDataRequestFormTemplate.bind(json);
   ```
   
   **NEW (mostly same, but tested with new Play)**
   ```java
   Form<KpiDataRequest> kpiDataRequestForm = 
       kpiDataRequestFormTemplate.bind(json);
   // No change needed, but verify with new Play version
   ```

5. **Update Test Cases**
   ```java
   // Use new Test helper class
   import play.test.Helpers;
   import play.mvc.Result;
   
   @Test
   public void testEndpoint() {
       Result result = route(new FakeRequest(
           GET, "/some/path"
       ));
       assertEquals(OK, result.status());
   }
   ```

**Day 23-25 Deliverables:**
- All Promise replaced with CompletableFuture
- play.Play.application() removed
- Dependency injection used throughout
- Controllers tested and working

---

### Day 26-27: Play 2.8 → 3.0 Final Upgrade

**Objectives:**
- Update to Play 3.0.x
- Verify all integrations work
- Update configuration

**Tasks:**

1. **Update Play Version to 3.0**
   ```xml
   <play.version>3.0.5</play.version>
   
   <!-- Update all Play dependencies to 3.0.5 -->
   <dependency>
       <groupId>com.typesafe.play</groupId>
       <artifactId>play_2.13</artifactId>
       <version>3.0.5</version>
   </dependency>
   ```

2. **Update Play Configuration** (`application.conf`)
   ```conf
   # Play 3.0 specific settings
   play.modules.enabled += "play.api.inject.guice.GuiceModule"
   
   # Enable HTTP/2 if desired
   play.server.provider = play.core.server.NettyServerProvider
   
   # Update any deprecated properties
   # Reference: https://www.playframework.com/documentation/3.0.x/Migration30
   ```

3. **Test Full Build**
   ```bash
   mvn clean install
   # This will take longer as Play 3.0 has more strict compilation
   ```

4. **Address Any Remaining Errors**
   - Handle deprecated method warnings
   - Update any custom Play plugins
   - Test all application endpoints

5. **Update Docker Build** (preliminary)
   - Update build scripts to use Java 11+
   - Update maven version in Docker
   - Test docker build

**Day 26-27 Deliverables:**
- Play Framework 3.0.5 integrated
- All compilation successful
- Application boots without errors
- Core functionality tested

---

## Phase 4: Java 11 → Java 21 Migration (8 days)

### Day 28: Prepare Java 21 Environment

**Objectives:**
- Install Java 21 tools
- Update build configuration
- Create compatibility tests

**Tasks:**

1. **Install Java 21 SDK**
   ```bash
   sdk install java 21.0.1-oracle
   ```

2. **Update pom.xml for Java 21**
   ```xml
   <java.source>21</java.source>
   <java.target>21</java.target>
   <maven.compiler.release>21</maven.compiler.release>
   ```

3. **Enable Java 21 Features** (optional, for modernization)
   ```java
   // Java 21 introduces sealed classes, records, pattern matching
   // Can be used gradually in new code
   
   // Example: Record instead of data class
   record Actor(Long id, String name, String email) { }
   ```

4. **Compilation Test**
   ```bash
   mvn clean compile
   # Should succeed with Java 21
   ```

**Day 28 Deliverables:**
- Java 21 SDK installed and configured
- pom.xml updated for Java 21
- Compilation successful

---

### Day 29-30: Java 21 Runtime Testing

**Objectives:**
- Test application with Java 21
- Identify any runtime issues
- Update Docker base image

**Tasks:**

1. **Update Docker Base Image for Build**
   ```dockerfile
   # OLD (development-bizdock-image/Dockerfile)
   FROM centos:7
   RUN yum install -y java-1.8.0-openjdk-devel
   
   # NEW
   FROM ubuntu:22.04
   RUN apt-get update && apt-get install -y \
       openjdk-21-jdk \
       maven \
       git \
       wget
   ```

2. **Run Application in Docker with Java 21**
   ```bash
   docker build -t bizdock:java21-test .
   docker run -it bizdock:java21-test \
       bash -c "./opt/prepare/build.sh && ./opt/prepare/db.sh -r"
   ```

3. **Test Key Workflows**
   - User login
   - Create portfolio entry
   - API calls
   - Report generation
   - Database operations

4. **Monitor for Warnings**
   - Check logs for deprecation warnings
   - Address any Java 21 incompatibilities
   - Update any JVM flags if needed

**Day 29-30 Deliverables:**
- Java 21 runtime verified
- Application functional with Java 21
- Docker image for Java 21 tested

---

### Day 31-32: Performance & Security Validation

**Objectives:**
- Validate performance
- Run security checks
- Ensure compliance

**Tasks:**

1. **Performance Testing**
   ```bash
   # Load testing with existing benchmarks
   # Monitor memory usage with Java 21
   # Check startup time improvements
   
   # Example: Basic load test
   ab -n 1000 -c 10 http://localhost:8080/
   ```

2. **Security Audit**
   ```bash
   # Run OWASP dependency check
   mvn org.owasp:dependency-check-maven:check
   
   # Expected: Should find fewer vulnerabilities with updated libraries
   ```

3. **Compatibility Check**
   - Test with supported browsers
   - Verify API endpoints with clients
   - Check third-party integrations

4. **Document Changes**
   - Create release notes
   - Document performance improvements
   - List known issues (if any)

**Day 31-32 Deliverables:**
- Performance validated
- Security audit passed
- Compatibility confirmed
- Release notes prepared

---

## Phase 5: Docker Modernization (5 days)

### Day 33-34: Update Docker Base Images

**Objectives:**
- Migrate from CentOS 7 to Ubuntu 22.04
- Update all build scripts
- Test Docker builds

**Tasks:**

1. **Update bizdock-docker Development Image**
   
   **File:** `development-bizdock-image/Dockerfile`
   
   ```dockerfile
   # OLD
   FROM centos:7
   
   RUN yum update -y && yum install -y --setopt=tsflags=nodocs epel-release && yum clean all
   
   RUN yum install -y --setopt=tsflags=nodocs \
       java-1.8.0-openjdk-devel \
       sudo \
       mariadb \
       git \
       wget \
       unzip \
       bc
   
   # NEW
   FROM ubuntu:22.04
   
   ENV DEBIAN_FRONTEND=noninteractive
   ENV LANG=en_US.UTF-8
   
   RUN apt-get update && apt-get install -y --no-install-recommends \
       openjdk-21-jdk \
       sudo \
       mariadb-client \
       git \
       wget \
       unzip \
       bc \
       curl \
       ca-certificates \
       && rm -rf /var/lib/apt/lists/*
   ```

2. **Update Installation Scripts**
   
   **Review:** `development-bizdock-image/install_play.sh`
   
   ```bash
   # May need updates for Ubuntu
   # play framework installation should still work
   # wget-based download should be compatible
   ```

3. **Update Build Scripts**
   
   **Review:** `development-bizdock-image/build.sh`
   
   ```bash
   # Should be mostly compatible
   # Any yum commands need conversion to apt-get
   # Test thoroughly
   ```

4. **Update bizdock-installation Images**
   
   **File:** `bizdock/Dockerfile` and `bizdockdb/Dockerfile`
   
   ```dockerfile
   # OLD
   FROM centos:7
   FROM mariadb:10.1
   
   # NEW
   FROM ubuntu:22.04
   FROM mariadb:11.0-jammy  # Ubuntu 22.04 LTS variant
   ```

5. **Test Docker Builds**
   ```bash
   # Build development image
   cd bizdock-docker/development-bizdock-image
   docker build -t bizdock-dev:java21 .
   
   # Test run
   docker run -it bizdock-dev:java21 bash
   
   # Build deployment images
   cd bizdock-installation
   docker build -t bizdock-app:3.0 ./bizdock
   docker build -t bizdock-db:3.0 ./bizdockdb
   
   # Test compose
   docker-compose up
   ```

**Day 33-34 Deliverables:**
- Dockerfiles updated to Ubuntu 22.04
- All installation scripts compatible
- Docker images build successfully
- Docker Compose verified

---

### Day 35-36: Update Docker Compose & Scripts

**Objectives:**
- Update docker-compose configuration
- Update startup and management scripts
- Test full stack deployment

**Tasks:**

1. **Update docker-compose.yml** (if exists, or create one)
   ```yaml
   version: '3.8'
   
   services:
     bizdock-db:
       image: mariadb:11.0-jammy
       environment:
         MYSQL_ROOT_PASSWORD: root
         MYSQL_DATABASE: bizdock
       volumes:
         - db_data:/var/lib/mysql
       ports:
         - "3306:3306"
   
     bizdock-app:
       image: bizdock-app:3.0
       depends_on:
         - bizdock-db
       environment:
         DATABASE_URL: "jdbc:mysql://bizdock-db:3306/bizdock"
         JAVA_TOOL_OPTIONS: "-XX:+UseG1GC -Xmx2G"
       ports:
         - "8080:8080"
       volumes:
         - app_data:/opt/maf/
   
   volumes:
     db_data:
     app_data:
   ```

2. **Update Startup Scripts**
   
   **Review:** `bizdock/startup.sh` and `bizdockdb/startup.sh`
   
   ```bash
   # Verify scripts work with new Ubuntu base
   # Update any package manager references
   # Ensure Java 21 is properly detected
   ```

3. **Update Environment Detection**
   
   ```bash
   # Ensure JAVA_HOME is set correctly
   # Check for Ubuntu-specific paths
   
   # In startup.sh:
   export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
   ```

4. **Test Full Stack Deployment**
   ```bash
   # Build all images
   docker-compose build
   
   # Start services
   docker-compose up -d
   
   # Wait for startup
   sleep 10
   
   # Test application
   curl http://localhost:8080
   
   # Check logs
   docker-compose logs -f bizdock-app
   ```

5. **Documentation Update**
   
   **Update:** `README.md` files
   
   - System requirements: Java 21, Docker 24+, Docker Compose 2+
   - Instructions for Ubuntu host
   - Troubleshooting section for new stack

**Day 35-36 Deliverables:**
- docker-compose.yml created/updated
- Startup scripts updated
- Full stack tested and working
- Documentation updated

---

### Day 37: Database Migration & Deployment

**Objectives:**
- Ensure database compatibility
- Test migrations
- Document deployment steps

**Tasks:**

1. **Verify MariaDB 11.0 Compatibility**
   ```sql
   -- Test basic operations
   SELECT VERSION();
   
   -- Verify character sets
   SHOW VARIABLES LIKE 'character_set%';
   
   -- Check for any deprecated features
   ```

2. **Update Database Initialization Scripts**
   
   **Review:** `bizdock-installation/bizdockdb/startup.sh`
   
   ```bash
   # Update MySQL configuration for MariaDB 11.0
   # Check my.cnf settings
   # Ensure UTF-8 defaults
   ```

3. **Test Database Migration Path**
   ```bash
   # Backup current database
   mysqldump bizdock > backup.sql
   
   # Test restore with new MariaDB 11.0
   mysql bizdock < backup.sql
   
   # Verify data integrity
   SELECT COUNT(*) FROM [tables];
   ```

4. **Create Deployment Checklist**
   ```markdown
   ## Deployment Checklist
   
   - [ ] Java 21 installed and verified
   - [ ] Maven 3.9+ configured
   - [ ] Docker 24+ and Docker Compose 2+ installed
   - [ ] Source code cloned from migration branches
   - [ ] All submodules updated
   - [ ] Build completes successfully
   - [ ] Database created and initialized
   - [ ] Application starts without errors
   - [ ] Login works
   - [ ] API endpoints respond
   - [ ] Reports generate
   - [ ] Performance acceptable
   - [ ] Security audit passed
   ```

5. **Prepare Rollback Plan**
   ```bash
   # Document steps to rollback if issues found
   - Keep previous Java 8 / Play 2.4 branches
   - Backup database before migration
   - Version control all images
   - Document any data transformations
   ```

**Day 37 Deliverables:**
- Database compatibility verified
- Migration path tested
- Deployment checklist created
- Rollback plan documented

---

## Phase 6: Integration Testing & Finalization (10 days)

### Day 38-40: Comprehensive Testing

**Objectives:**
- Run full test suite
- Test all critical paths
- Document known issues

**Tasks:**

1. **Unit Test Suite**
   ```bash
   mvn test -DskipIntegrationTests
   # Expected: 90%+ pass rate
   # Document any failures
   ```

2. **Integration Tests**
   ```bash
   mvn verify
   # Test database operations
   # Test API endpoints
   # Test authentication flows
   ```

3. **Manual Testing Checklist**
   
   | Feature | Status | Notes |
   |---------|--------|-------|
   | User Login | Pass/Fail | SSO, SAML, Local |
   | Portfolio Management | Pass/Fail | Create, Edit, Delete |
   | Report Generation | Pass/Fail | All report types |
   | API Endpoints | Pass/Fail | All versions |
   | File Upload/Download | Pass/Fail | PDF, Excel, etc |
   | Dashboard | Pass/Fail | Widgets, refresh |
   | Notifications | Pass/Fail | Email sending |
   | Search | Pass/Fail | Full-text search |

4. **Performance Testing**
   ```bash
   # Load testing
   # Memory profiling
   # Startup time measurement
   # Database query performance
   ```

5. **Browser Compatibility**
   - Chrome 120+
   - Firefox 121+
   - Safari 17+
   - Edge 120+

**Day 38-40 Deliverables:**
- Test results documented
- All major functionality verified
- Known issues listed with workarounds
- Performance baseline established

---

### Day 41-42: Documentation & Migration Guide

**Objectives:**
- Create comprehensive migration documentation
- Document known issues
- Create troubleshooting guide

**Tasks:**

1. **Create UPGRADE_GUIDE.md**
   ```markdown
   # Upgrade Guide: BizDock from Play 2.4 to Play 3.0
   
   ## Prerequisites
   - Java 21 LTS or higher
   - Maven 3.9+
   - Docker 24+
   - Docker Compose 2+
   
   ## Upgrade Steps
   1. Backup existing database
   2. Update source code from migration branches
   3. Build with: mvn clean install
   4. Deploy using new Docker images
   5. Run database migrations
   6. Verify all functionality
   
   ## Troubleshooting
   [...]
   ```

2. **Create KNOWN_ISSUES.md**
   ```markdown
   # Known Issues in Play 3.0 Migration
   
   ## Issue 1: [Description]
   - Status: [Open/Resolved]
   - Workaround: [If applicable]
   - Timeline: [Planned fix date]
   
   [...]
   ```

3. **Create BREAKING_CHANGES.md**
   ```markdown
   # Breaking Changes
   
   ## Database
   - MySQL Connector 8.0 requires different connection properties
   - MariaDB 11.0 has stricter SQL mode
   
   ## API
   - Swagger documentation moved to `/api-docs`
   - Response format may differ slightly
   
   ## Configuration
   - `play.Play.application()` no longer available
   - Must use dependency injection
   
   [...]
   ```

4. **Create ARCHITECTURE_NOTES.md**
   ```markdown
   # Architecture Changes in Play 3.0 Migration
   
   ## ORM: Ebean 13.x
   - Fully JPA compatible
   - Query API improved
   - Better performance
   
   ## Build System
   - Maven still supported
   - Plan migration to Gradle for Phase 7
   - SBT working for Play compilation
   
   ## Security
   - Updated PAC4j for better auth
   - All javax->jakarta migrations
   
   [...]
   ```

**Day 41-42 Deliverables:**
- Comprehensive migration documentation
- Known issues documented
- Troubleshooting guide created
- Architecture notes prepared

---

### Day 43-45: Merge & Release Preparation

**Objectives:**
- Merge branches to main
- Create release branch
- Prepare release

**Tasks:**

1. **Branch Merge Strategy**
   ```bash
   # Merge feature branches into migration branch
   git checkout migration/java21-play3.0
   
   git merge migration/play3.0/models
   git merge migration/play3.0/controllers
   git merge migration/play3.0/services
   # ... other feature branches
   
   # Create release branch
   git checkout -b release/3.0.0
   
   # Bump version numbers
   mvn versions:set -DnewVersion=3.0.0
   git commit -am "Release 3.0.0"
   ```

2. **Tag Release**
   ```bash
   git tag -a v3.0.0 -m "Release 3.0.0: Java 21, Play 3.0"
   git push --tags
   ```

3. **Create Release Notes**
   ```markdown
   # BizDock 3.0.0 Release Notes
   
   ## Major Changes
   - Upgraded from Java 8 to Java 21 LTS
   - Migrated from Play 2.4.2 to Play 3.0.5
   - Updated to Ebean ORM 13.x
   - Modernized Docker base images (Ubuntu 22.04)
   - Updated all security frameworks (PAC4j, Swagger)
   
   ## Improvements
   - Better performance with modern Java features
   - Improved security with updated libraries
   - Enhanced reliability with LTS releases
   - Streamlined architecture
   
   ## Breaking Changes
   [...]
   
   ## Migration Path
   See UPGRADE_GUIDE.md
   
   ## Thanks
   All contributors to this major migration effort.
   ```

4. **Prepare Docker Hub / Registry Push**
   ```bash
   # Tag images for release
   docker tag bizdock-app:java21 yourregistry/bizdock-app:3.0.0
   docker tag bizdock-db:3.0 yourregistry/bizdock-db:3.0.0
   
   # Push to registry
   docker push yourregistry/bizdock-app:3.0.0
   docker push yourregistry/bizdock-db:3.0.0
   ```

5. **Update Maven Central** (if applicable)
   ```bash
   # For app-framework library
   mvn clean deploy -P release
   ```

**Day 43-45 Deliverables:**
- All branches merged successfully
- Release version tagged
- Release notes published
- Docker images pushed to registry
- Maven artifacts deployed

---

## Timeline Summary

```
Phase 1: Assessment & Preparation          Days 1-5    (5 days)
  └─ Days 1-2: Codebase Analysis
  └─ Day 3: Environment Setup
  └─ Days 4-5: Database & Dependency Analysis

Phase 2: Intermediate Upgrades             Days 6-10   (5 days)
  └─ Day 6: Java 8 → Java 11
  └─ Day 7: MySQL Connector & Dependencies
  └─ Day 8: javax.* → jakarta.* Migration
  └─ Day 9: PAC4j & Security Updates
  └─ Day 10: Scala & Play 2.4 Build System

Phase 3: Play Framework Migration          Days 11-27  (17 days)
  └─ Days 11-12: Play 2.4 → 2.8
  └─ Days 13-14: Plan Play 3.0 Migration
  └─ Days 15-19: Ebean ORM Migration (Core)
  └─ Days 20-22: Swagger API Update
  └─ Days 23-25: Controller & Routing Migration
  └─ Days 26-27: Play 2.8 → 3.0 Final Upgrade

Phase 4: Java 11 → 21 Migration            Days 28-32  (5 days)
  └─ Day 28: Prepare Java 21 Environment
  └─ Days 29-30: Java 21 Runtime Testing
  └─ Days 31-32: Performance & Security Validation

Phase 5: Docker Modernization              Days 33-37  (5 days)
  └─ Days 33-34: Update Docker Base Images
  └─ Days 35-36: Update Docker Compose & Scripts
  └─ Day 37: Database Migration & Deployment

Phase 6: Integration Testing & Release     Days 38-45  (8 days)
  └─ Days 38-40: Comprehensive Testing
  └─ Days 41-42: Documentation & Migration Guide
  └─ Days 43-45: Merge & Release Preparation

TOTAL: 45-50 DAYS (accounting for buffer time)
Recommended: 60-70 days with parallel team work
```

---

## Resource Requirements

### Team Composition (Recommended)

| Role | Count | Tasks |
|------|-------|-------|
| Lead Developer | 1 | Overall architecture, Play 3.0 migration, review |
| Backend Developer 1 | 1 | Ebean ORM migration, DAO updates |
| Backend Developer 2 | 1 | Controller migration, configuration |
| DevOps Engineer | 1 | Docker updates, deployment testing |
| QA Lead | 1 | Test planning, testing coordination |
| QA Tester | 2 | Manual testing, compatibility testing |

**Total: 6-7 people for parallel execution**

### Infrastructure Requirements

- Build Server: 16 GB RAM, 8 CPU cores, 200 GB SSD
- Test Database: MySQL 8.0 / MariaDB 11.0 instances
- Docker Registry: Private or public (Docker Hub)
- Test Environment: 3x servers (Dev, Staging, Production-like)
- Monitoring: Grafana/Prometheus for performance testing

---

## Risk Mitigation

### High-Risk Areas

1. **Ebean ORM Migration**
   - Risk: Complex queries may break
   - Mitigation: Comprehensive testing, gradual rollout
   - Rollback: Keep old Ebean version available

2. **Play Framework 3.0**
   - Risk: Major architectural changes
   - Mitigation: Extensive integration testing
   - Rollback: Maintain Play 2.8 branch

3. **Database Driver Update**
   - Risk: Connection issues
   - Mitigation: Thorough connection pool testing
   - Rollback: Keep MySQL 5.1.36 driver available

### Testing Strategy

- Unit Tests: 90%+ coverage required
- Integration Tests: All critical paths
- Regression Tests: Before/after performance comparison
- User Acceptance Testing: Key stakeholders
- Production Readiness: Checklist review

---

## Success Criteria

✅ **Migrate Successfully When:**

- [ ] All unit tests pass (90%+ success rate)
- [ ] All integration tests pass
- [ ] Performance equal or better than previous version
- [ ] Security audit passes (zero critical vulnerabilities)
- [ ] No data loss during migration
- [ ] All APIs functioning correctly
- [ ] User login and authentication working
- [ ] Reports generating without errors
- [ ] Documentation complete
- [ ] Team trained on new stack

---

## Post-Migration (Future Phases)

### Phase 7: Gradle Migration (10 days)
- Replace Maven with Gradle
- Modernize build pipeline
- Improve build performance

### Phase 8: Spring Boot Evaluation (15 days)
- Evaluate Spring Boot as alternative to Play
- Cost-benefit analysis
- Prototype migration if beneficial

### Phase 9: Containerization Improvements (5 days)
- Optimize Docker image sizes
- Implement multi-stage builds
- Add Kubernetes support

---

## Conclusion

This migration positions BizDock on modern, long-term supported versions of all critical dependencies. While the 60-70 day timeline is substantial, it's necessary given the age of the current stack. The phased approach allows for parallel work, testing at each stage, and provides clear rollback paths if issues arise.

**Key Success Factors:**
1. Strong test coverage throughout
2. Parallel team work on distinct phases
3. Regular communication and progress tracking
4. Thorough documentation at each step
5. Production-like staging environment for testing
6. Clear rollback procedures maintained

**Questions? Refer to:**
- Phase-specific sections above
- MIGRATION_CHECKLIST.md (in repository)
- Team wiki for ongoing status updates
