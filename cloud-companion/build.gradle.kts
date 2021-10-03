import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform.getCurrentOperatingSystem

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
  implementation("org.apache.tika:tika-core:2.1.0")
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

tasks.compileKotlin {
  kotlinOptions {
    freeCompilerArgs = listOf("-Xjsr305=strict")
    jvmTarget = "1.8"
  }
}

tasks.test {

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
  // Creating the full all-in-one image containing all compilers is hidden behind a build flag because
  // the final image will be HUGE (~15GB). By default, this will build a minimal image based on
  // adoptopenjdk:8-jre-hotspot. You can enable the all-in-one build by passing the
  // "companion-build-aio" flag to gradle:
  // ./gradlew :cloud-companion:jib[DockerBuild] -P companion-build-aio
  val buildAio = project.hasProperty("companion-build-aio")
  if (buildAio) {
    project.logger.info("Build companion all-in-one image. Build might take while...")
  }

  from {
    image = if (buildAio) {
      "docker.io/replco/polygott:9180d8b24b181125577e98a8f5418781e863e852"
    } else {
        // adoptopenjdk:8-jre-hotspot
      "docker.io/library/adoptopenjdk@sha256:f89b4c0ee78145a575df40017b12003d86ed877c4a68bd063f7ca0221ff8643b"
    }
  }

  to {
    image = "ghcr.io/codefreak/codefreak-cloud-companion"

    tags = if (buildAio) {
      setOf("aio")
    } else {
      setOf("minimal")
    }
  }
  container {
    user = "1000:1000"
    workingDirectory = "/home/runner"
    labels.put("org.opencontainers.image.source", "https://github.com/codefreak/codefreak")
    environment = mapOf(
      "HOME" to "" // this is set by the polygott base image but confuses the bash environment
    )
  }
  // The files from src/main/jib are added by default.
  // These files are there to harmonize the underlying image and ensure a user named "runner"
  // is present with the uid:git 1000:1000; e.g. the user gets a default bash configuration.
  extraDirectories {
    permissions = mapOf(
      "/etc/{passwd,group,shadow}" to "644"
    )
  }
  pluginExtensions {
    pluginExtension {
      implementation = "com.google.cloud.tools.jib.gradle.extension.springboot.JibSpringBootExtension"
    }
    pluginExtension {
      implementation = "com.google.cloud.tools.jib.gradle.extension.ownership.JibOwnershipExtension"
      configuration( Action<com.google.cloud.tools.jib.gradle.extension.ownership.Configuration> {
        rules {
          rule {
            glob = "{/home/runner,/home/runner/**}"
            ownership = "1000:1000"
          }
        }
      })
    }
  }
}

spotless {
  kotlin {
    ktlint().userData(mapOf("indent_size" to "2"))
  }
}
