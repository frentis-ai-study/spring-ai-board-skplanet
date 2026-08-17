plugins {
    java
    id("org.springframework.boot") version "3.5.0"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "com.frentis"
version = "0.0.1-SNAPSHOT"
description = "SK플래닛 Spring AI 지능형 웹 실무 — step6 (종합 Agent + 프로덕션)"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

extra["springAiVersion"] = "1.1.7"
extra["resilience4jVersion"] = "2.2.0"

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-web")
    runtimeOnly("com.h2database:h2")

    // Spring AI Chat (OpenAI)
    implementation("org.springframework.ai:spring-ai-starter-model-openai")
    implementation("org.springframework.ai:spring-ai-starter-model-chat-memory-repository-jdbc")
    // Vector Store: SimpleVectorStore는 spring-ai-core 내장이라 별도 스타터가 필요 없다
    implementation("org.springframework.ai:spring-ai-advisors-vector-store")
    implementation("org.springframework.ai:spring-ai-rag")

    // 운영 안정화
    implementation("io.github.resilience4j:resilience4j-spring-boot3:${property("resilience4jVersion")}")
    implementation("io.micrometer:micrometer-registry-prometheus")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testRuntimeOnly("com.h2database:h2")
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.ai:spring-ai-bom:${property("springAiVersion")}")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}
