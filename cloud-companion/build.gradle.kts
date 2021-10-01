import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform.getCurrentOperatingSystem
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  id("org.springframework.boot")
  id("io.spring.dependency-management")
  id("org.jetbrains.kotlin.kapt")
  id("com.google.cloud.tools.jib")
  id("com.diffplug.spotless")
  kotlin("jvm")
  kotlin("plugin.spring")
}

group = "org.codefreak.cloud"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_1_8

configurations {
  compileOnly {
    extendsFrom(configurations.annotationProcessor.get())
  }
}

repositories {
  mavenCentral()
  maven(url = "https://packages.jetbrains.team/maven/p/ij/intellij-dependencies")
  maven(url = "https://repo.spring.io/snapshot") // for graphql-spring-boot-starter until it's in stable
}

dependencies {
  implementation("org.springframework.boot:spring-boot-starter-webflux")
  implementation("org.springframework.boot:spring-boot-starter-actuator")
  implementation("org.springframework.boot:spring-boot-starter-aop")
  implementation("org.springframework.experimental:graphql-spring-boot-starter:1.0.0-SNAPSHOT")
  implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
  implementation("io.projectreactor.kotlin:reactor-kotlin-extensions")
  implementation("org.jetbrains.kotlin:kotlin-reflect")
  implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")

  implementation("commons-io:commons-io:2.11.0")
  implementation("org.apache.commons:commons-compress:1.21")
  implementation("org.apache.tika:tika-core:1.27")
  // https://packages.jetbrains.team/maven/p/ij/intellij-dependencies/org/jetbrains/pty4j/pty4j/
  implementation("org.jetbrains.pty4j:pty4j:0.11.5")

  developmentOnly("org.springframework.boot:spring-boot-devtools")
  annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
  testImplementation("org.springframework.boot:spring-boot-starter-test") {
    // we are using Hamcrest matchers and assertj's "assertThat" is annoying in autocompletion
    exclude(group = "org.assertj", module = "assertj-core")
  }
  testImplementation("com.fasterxml.jackson.module:jackson-module-kotlin")
  testImplementation("org.springframework.graphql:spring-graphql-test:1.0.0-SNAPSHOT")
  testImplementation("junit:junit:4.13.2")
  testImplementation("io.projectreactor:reactor-test")
}

tasks.withType<KotlinCompile> {
  kotlinOptions {
    freeCompilerArgs = listOf("-Xjsr305=strict")
    jvmTarget = "1.8"
  }
}

tasks.withType<Test> {

  // The tests are somehow broken on Windows. Especially the file-related
  // tests tend to fail deleting files. Maybe somewhere are leaking file
  // descriptors...?
  onlyIf { !getCurrentOperatingSystem().isWindows }

  useJUnitPlatform()
  testLogging {
    events = setOf(TestLogEvent.FAILED)
    exceptionFormat = TestExceptionFormat.FULL
  }
}

jib {
  from {
    //image = "replco/polygott:c82c08a720ba1fd537d4fba17eed883ab87c0fd7"
  }
  to {
    image = "ghcr.io/codefreak/codefreak-cloud-companion"
  }
  container {
    volumes = listOf(
      "/code"
    )
    labels.put("org.opencontainers.image.source", "https://github.com/codefreak/codefreak-cloud-companion")
  }
  pluginExtensions {
    pluginExtension {
      implementation = "com.google.cloud.tools.jib.gradle.extension.springboot.JibSpringBootExtension"
    }
  }
}

spotless {
  kotlin {
    ktlint().userData(mapOf("indent_size" to "2"))
  }
}
