plugins {
    `java-library`
    alias(libs.plugins.lombok)
    alias(libs.plugins.dependencyManagement)
    id("io.github.bitfist.github.release")
}

group = "io.github.bitfist"

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    api(platform(libs.springBootBom))
    api("org.springframework.boot:spring-boot-autoconfigure")
    api("org.springframework.boot:spring-boot-starter-validation")
    api("com.fasterxml.jackson.core:jackson-databind")
    api("org.slf4j:slf4j-api")
    api(libs.jcef)

    implementation(platform(libs.springModulithBom))
    implementation("org.springframework.modulith:spring-modulith-core")

    // region Test
    testImplementation(platform(libs.springModulithBom))
    testImplementation("org.springframework.modulith:spring-modulith-starter-test")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation(libs.compileTesting)
    testImplementation(libs.googleTruth)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    // endregion

    annotationProcessor(libs.springBootAutoconfigureProcessor)
    annotationProcessor(libs.springBootConfigurationProcessor)
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.compilerArgs.add("-parameters")
}

tasks.withType(Javadoc::class.java).configureEach {
    (options as CoreJavadocOptions).addStringOption("Xdoclint:-missing", "-quiet")
}

tasks.test {
    useJUnitPlatform()
    jvmArgs(
        "--add-exports=jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED",
        "--add-exports=jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED",
        "--add-exports=jdk.compiler/com.sun.tools.javac.main=ALL-UNNAMED"
    )
}

gitHubRelease {
    projectName.set("JCEF Spring Boot Starter")
    projectDescription.set("Spring Boot Starter for JCEF")
    developer.set("bitfist")
    licenseFile.set(projectDir.resolve("LICENSE.txt"))
}

