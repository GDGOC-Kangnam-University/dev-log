plugins {
	kotlin("jvm") version "2.1.10"
	alias(libs.plugins.spring.boot)
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
	mavenCentral()
}

dependencies {
	testImplementation(kotlin("test"))
	implementation(libs.bundles.database)
	implementation(libs.bundles.spring)
	implementation(libs.bundles.coroutines)
}

tasks.test {
	useJUnitPlatform()
}
kotlin {
	jvmToolchain(21)
}