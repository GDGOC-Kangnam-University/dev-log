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
	runtimeOnly(libs.h2.jdbc)
	testImplementation(libs.spring.boot.starter.test)
}

tasks.test {
	useJUnitPlatform()
}

java {
	toolchain {
		languageVersion.set(JavaLanguageVersion.of(21))
	}
}
