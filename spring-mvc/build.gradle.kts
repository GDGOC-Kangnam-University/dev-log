plugins {
	java
	alias(libs.plugins.spring.boot)
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
	mavenCentral()
}

dependencies {
	implementation(libs.bundles.spring)
	implementation(libs.spring.boot.starter.data.jpa)
	runtimeOnly(libs.database.runtime)
	@Suppress("VulnerableLibrariesLocal")
	testImplementation(libs.spring.boot.starter.test)
	compileOnly(libs.lombok)
	annotationProcessor(libs.lombok)
}

tasks.test {
	useJUnitPlatform()
}

java {
	toolchain {
		languageVersion.set(JavaLanguageVersion.of(21))
	}
}
