val ktor_version: String by project
val kotlin_version: String by project
val logback_version: String by project

val exposed_version: String by project
val h2_version: String by project
plugins {
    kotlin("jvm") version "1.9.10"
    id("io.ktor.plugin") version "2.3.4"
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.10"
}

group = "net.clynamic"
version = "0.0.1"

application {
    mainClass.set("net.clynamic.ApplicationKt")

    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

kotlin {
    jvmToolchain(11)
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("ch.qos.logback:logback-classic:$logback_version")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.15.+")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.15.+")
    implementation("com.h2database:h2:$h2_version")
    implementation("com.squareup.okhttp3:okhttp:4.10.0")
    implementation("io.swagger.core.v3:swagger-annotations:2.2.20")
    implementation("io.github.cdimascio:dotenv-kotlin:6.4.1")
    implementation("io.github.smiley4:ktor-swagger-ui:2.4.0")
    implementation("io.ktor:ktor-serialization-jackson:$ktor_version")
    implementation("io.ktor:ktor-server-auth:$ktor_version")
    implementation("io.ktor:ktor-server-content-negotiation-jvm")
    implementation("io.ktor:ktor-server-core-jvm")
    implementation("io.ktor:ktor-server-cors:$ktor_version")
    implementation("io.ktor:ktor-server-netty-jvm")
    implementation("io.ktor:ktor-server-status-pages:$ktor_version")
    implementation("org.jetbrains.exposed:exposed-core:$exposed_version")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposed_version")
    implementation("org.jsoup:jsoup:1.14.3")
    implementation("org.slf4j:slf4j-api:2.0.9")
    implementation("org.xerial:sqlite-jdbc:3.42.0.1")
    testImplementation("io.ktor:ktor-server-tests-jvm")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlin_version")
}
