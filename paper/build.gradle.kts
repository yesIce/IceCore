plugins {
    `java-library`
    id("com.gradleup.shadow")
}

dependencies {
    implementation(project(":core"))
    implementation(project(":common"))

    compileOnly(libs.paper.api)
}

tasks.processResources {
    val props = mapOf(
        "version" to version,
        "name" to "IceCore"
    )
    inputs.properties(props)
    filesMatching("plugin.yml") {
        expand(props)
    }
}

tasks.shadowJar {
    archiveClassifier.set("")
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
    exclude(
        "META-INF/*LICENSE*",
        "META-INF/*NOTICE*",
        "META-INF/README.txt",
        "META-INF/DEPENDENCIES"
    )

    val base = "com.wiceh.icecore.libs"
    relocate("com.zaxxer.hikari", "$base.hikari")
    relocate("redis.clients.jedis", "$base.jedis")
    relocate("org.apache.commons.pool2", "$base.commons.pool2")
    relocate("com.google.gson", "$base.gson")

    mergeServiceFiles()
}

tasks.build {
    dependsOn(tasks.shadowJar)
}

tasks.jar {
    enabled = false
}
