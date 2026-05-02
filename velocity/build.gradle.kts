plugins {
    `java-library`
    id("com.gradleup.shadow")
}

dependencies {
    implementation(project(":core"))
    implementation(project(":common"))

    compileOnly(libs.velocity.api)
    annotationProcessor(libs.velocity.api)

    testImplementation(libs.bundles.testing)
}

tasks.shadowJar {
    archiveClassifier.set("")
    relocate("com.zaxxer.hikari", "net.tuonome.networkcore.libs.hikari")
    relocate("io.lettuce", "net.tuonome.networkcore.libs.lettuce")
}

tasks.build {
    dependsOn(tasks.shadowJar)
}
