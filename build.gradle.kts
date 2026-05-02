plugins {
    `java-library`
    id("com.gradleup.shadow") version "9.4.1" apply false
}

allprojects {
    group = "com.wiceh.icecore"
    version = "1.0.0-SNAPSHOT"

    repositories {
        mavenCentral()
        maven("https://repo.papermc.io/repository/maven-public/")
    }
}

subprojects {
    apply(plugin = "java-library")

    extensions.configure<JavaPluginExtension> {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(21))
        }
    }

    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
        options.release.set(21)
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }

    tasks.withType<Jar>().configureEach {
        archiveBaseName.set("IceCore-${project.name}")
    }
}
