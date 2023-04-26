import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
	id("org.springframework.boot") version "3.0.6"
	id("io.spring.dependency-management") version "1.1.0"
	kotlin("jvm") version "1.7.22"
	kotlin("plugin.spring") version "1.7.22"
}

val commercetools by extra("13.2.0")

group = "com.kingfisher"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_17

configurations {
	compileOnly {
		extendsFrom(configurations.annotationProcessor.get())
	}
}

repositories {
	mavenCentral()
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
	implementation("org.jetbrains.kotlin:kotlin-reflect")

	annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
	implementation("com.commercetools.sdk:commercetools-http-client:${commercetools}")
	implementation("com.commercetools.sdk:commercetools-sdk-java-api:${commercetools}")
	implementation("com.commercetools.sdk:commercetools-sdk-java-importapi:${commercetools}")
	implementation("com.commercetools.sdk:commercetools-sdk-java-ml:${commercetools}")
	implementation("com.commercetools.sdk:commercetools-sdk-java-history:${commercetools}")

	testImplementation("org.springframework.boot:spring-boot-starter-test")
}

tasks.withType<KotlinCompile> {
	kotlinOptions {
		freeCompilerArgs = listOf("-Xjsr305=strict")
		jvmTarget = "17"
	}
}

tasks.withType<Test> {
	useJUnitPlatform()
}
