plugins {
    `java-library`
}

dependencies {
    api(project(":common"))

    implementation(libs.bundles.database)
    implementation(libs.jedis)
    implementation(libs.slf4j.api)

    testImplementation(libs.bundles.testing)
    testRuntimeOnly(libs.slf4j.simple)
}
