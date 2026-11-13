import java.math.BigDecimal
import org.gradle.testing.jacoco.tasks.JacocoCoverageVerification
import org.gradle.testing.jacoco.tasks.JacocoReport

plugins {
	java
	jacoco
	id("org.springframework.boot") version "4.1.2"
	id("io.spring.dependency-management") version "1.1.7"
	id("org.sonarqube") version "7.5.0.8588"
}

group = "com.ceremo_app"
version = "0.0.1-SNAPSHOT"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(25)
	}
}

repositories {
	mavenCentral()
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-actuator")
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.springframework.boot:spring-boot-starter-flyway")
	implementation("org.springframework.boot:spring-boot-starter-security")
	implementation("org.springframework.boot:spring-boot-starter-webmvc")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
	implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
	implementation("org.springframework.boot:spring-boot-starter-restclient")
	implementation("org.aspectj:aspectjweaver")
	implementation("org.apache.commons:commons-lang3")
	implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:3.0.0")
	implementation("com.fasterxml.uuid:java-uuid-generator:5.1.1")
	implementation("org.springframework.boot:spring-boot-starter-cache")
	implementation("javax.cache:cache-api")
	implementation("org.ehcache:ehcache")
	implementation("org.hibernate.orm:hibernate-jcache")
	implementation("net.logstash.logback:logstash-logback-encoder:9.0")
	runtimeOnly("org.postgresql:postgresql")
	runtimeOnly("org.flywaydb:flyway-database-postgresql")
	testImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test")
	testImplementation("org.springframework.boot:spring-boot-starter-security-test")
	testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
	testImplementation("org.testcontainers:testcontainers-postgresql")
	testImplementation("com.tngtech.archunit:archunit:1.4.2")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
	testRuntimeOnly("com.h2database:h2")
}

tasks.withType<Test> {
	useJUnitPlatform()
}

val integrationTestSourceSet = sourceSets.create("integrationTest")

integrationTestSourceSet.compileClasspath += sourceSets.main.get().output
integrationTestSourceSet.runtimeClasspath += sourceSets.main.get().output

configurations[integrationTestSourceSet.implementationConfigurationName]
    .extendsFrom(configurations.testImplementation.get())

configurations[integrationTestSourceSet.runtimeOnlyConfigurationName]
    .extendsFrom(configurations.testRuntimeOnly.get())

val integrationTest = tasks.register<Test>("integrationTest") {
    description = "Runs integration tests against disposable services."
    group = "verification"

    testClassesDirs = integrationTestSourceSet.output.classesDirs
    classpath = integrationTestSourceSet.runtimeClasspath

    dependsOn(tasks.named(integrationTestSourceSet.classesTaskName))
    shouldRunAfter(tasks.named("test"))
}

tasks.named("check") {
    dependsOn(integrationTest)
}

jacoco {
    toolVersion = "0.8.15"
}

tasks.named<JacocoReport>("jacocoTestReport") {
    dependsOn(tasks.test)
    reports {
        html.required.set(true)
        xml.required.set(true)
    }
}

val combinedExecutionData = files(
    layout.buildDirectory.file("jacoco/test.exec"),
    layout.buildDirectory.file("jacoco/integrationTest.exec")
)

val verifyCoverageInputs = tasks.register("verifyCoverageInputs") {
    group = "verification"
    description = "Rejects absent or empty coverage data from either required test suite."
    dependsOn(tasks.test, integrationTest)
    doLast {
        check(combinedExecutionData.files.all { it.isFile && it.length() > 0 }) {
            "Both test suites must supply JaCoCo execution data."
        }
    }
}

val jacocoCombinedReport = tasks.register<JacocoReport>("jacocoCombinedReport") {
    group = "verification"
    description = "Reports coverage from both the regular and PostgreSQL test suites."
    dependsOn(verifyCoverageInputs)
    executionData(combinedExecutionData)
    sourceSets(sourceSets.main.get())
    reports {
        html.required.set(true)
        html.outputLocation.set(layout.buildDirectory.dir("reports/jacoco/combined/html"))
        xml.required.set(true)
        xml.outputLocation.set(layout.buildDirectory.file("reports/jacoco/combined/combined.xml"))
    }
}

val jacocoCombinedCoverageVerification = tasks.register<JacocoCoverageVerification>(
    "jacocoCombinedCoverageVerification"
) {
    group = "verification"
    description = "Requires zero missed lines and branches in every application class."
    dependsOn(jacocoCombinedReport)
    executionData(combinedExecutionData)
    sourceSets(sourceSets.main.get())
    violationRules {
        rule {
            element = "CLASS"
            limit {
                counter = "LINE"
                value = "MISSEDCOUNT"
                maximum = BigDecimal.ZERO
            }
            limit {
                counter = "BRANCH"
                value = "MISSEDCOUNT"
                maximum = BigDecimal.ZERO
            }
        }
    }
}

tasks.named("check") {
    dependsOn(jacocoCombinedCoverageVerification)
}

val sonarTestReport = tasks.register<Exec>("sonarTestReport") {
    group = "verification"
    description = "Maps both JUnit suites to Sonar's generic execution format."
    dependsOn(tasks.test, integrationTest)
    val python = providers.environmentVariable("CEREMO_PYTHON").orElse("python3")
    commandLine(python.get(), "scripts/sonar_test_report.py", "prepare")
}

sonar {
    properties {
        property("sonar.projectKey", "ceremo_backend")
        property("sonar.projectName", "CEREMO Backend")
        property("sonar.host.url", providers.environmentVariable("SONAR_HOST_URL")
            .orElse("http://localhost:9001").get())
        property("sonar.qualitygate.wait", true)
        property("sonar.tests", "src/test/java,src/integrationTest/java")
        property("sonar.java.test.binaries", "build/classes/java/test,build/classes/java/integrationTest")
        // Generic import replaces the duplicate Java/Kotlin JUnit importers.
        property("sonar.junit.reportPaths", "")
        property("sonar.testExecutionReportPaths", "build/reports/sonar/test-executions.xml")
        property("sonar.coverage.jacoco.xmlReportPaths", "build/reports/jacoco/combined/combined.xml")
    }
}

tasks.named("sonar") {
    dependsOn(sonarTestReport)
    dependsOn(tasks.named("check"))
}
