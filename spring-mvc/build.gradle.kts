plugins {
	alias(libs.plugins.kotlin)
	alias(libs.plugins.spring.boot)
	alias(libs.plugins.serialization)
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
	mavenCentral()
}

dependencies {
	testImplementation(kotlin("test"))
	implementation(libs.bundles.database)
	implementation(libs.bundles.spring) {
		exclude(group = "org.springframework.boot", module = "spring-boot-starter-json")
	}
	implementation(libs.bundles.coroutines)
	implementation(libs.bundles.serialization)
	implementation(libs.bundles.kotlin)
}

tasks.test {
	useJUnitPlatform()
}
kotlin {
	jvmToolchain(21)
}