import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

plugins { kotlin("jvm") version "2.1.0" apply false }
val upstream = rootDir.parentFile
val pinned = ProcessBuilder("git", "-C", upstream.path, "rev-parse", "HEAD").start().let {
    val revision = it.inputStream.bufferedReader().readText().trim()
    check(it.waitFor() == 0) { "Cannot determine USVM revision" }
    revision
}
check(System.getProperty("java.vendor") == "Eclipse Adoptium" &&
    System.getProperty("java.runtime.version") in setOf("21.0.9+10-LTS", "21.0.9+10")) {
    "Use Temurin 21.0.9+10 for the codeXek backend build"
}
// Audit changes are versioned in the pinned USVM fork.
subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "java-library")
    apply(plugin = "maven-publish")
    extensions.configure<PublishingExtension> {
        publications { create<MavenPublication>("backend") { from(components["java"]) } }
        repositories { maven { name = "Codexek"; url = uri(upstream.resolve("build/repository")) } }
    }
    group = "org.usvm"
    version = "0.0.0-codexek-$pinned"
    repositories { mavenCentral(); maven("https://jitpack.io") }
    val source = upstream.resolve(path.removePrefix(":").replace(':', '/'))
    extensions.configure<JavaPluginExtension> {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
        sourceSets.named("main") {
            java.setSrcDirs(listOf(source.resolve("src/main/java")))
            resources.setSrcDirs(listOf(source.resolve("src/main/resources")))
        }
    }
    extensions.configure<KotlinJvmProjectExtension> {
        sourceSets.named("main") {
            kotlin.setSrcDirs(listOf(source.resolve("src/main/kotlin")))
        }
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_1_8)
            freeCompilerArgs.add("-Xsam-conversions=class")
        }
    }
    tasks.withType<JavaCompile>().configureEach { options.encoding = "UTF-8" }
    tasks.withType<Jar>().configureEach {
        isPreserveFileTimestamps = false
        isReproducibleFileOrder = true
        from(listOf(upstream.resolve("LICENSE"), upstream.resolve("NOTICE.txt"))) { into("META-INF") }
        manifest.attributes("USVM-Revision" to pinned, "CX-Audit-Adapter" to "jvm-only-audit-v4")
    }
    dependencyLocking { lockAllConfigurations() }
    dependencies {
        "implementation"("org.jetbrains.kotlin:kotlin-stdlib:2.1.0")
        "implementation"("org.jetbrains.kotlin:kotlin-reflect:2.1.0")
        "implementation"("io.github.microutils:kotlin-logging:3.0.5")
        "implementation"("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.0")
    }
}
fun Project.lib(configuration: String, coordinate: String) = dependencies.add(configuration, coordinate)
fun Project.link(configuration: String, module: String) = dependencies.add(configuration, project(module))
val jacodb = "com.github.UnitTestBot.jacodb"
fun Project.jaco(configuration: String, name: String) = lib(configuration, "$jacodb:$name:4251d2023d")
project(":usvm-util") { lib("implementation", "org.jetbrains.kotlinx:kotlinx-collections-immutable-jvm:0.3.8") }
project(":usvm-core") {
    link("api", ":usvm-util")
    lib("api", "io.ksmt:ksmt-core:0.5.26")
    lib("api", "io.ksmt:ksmt-z3:0.5.26")
    lib("api", "org.jetbrains.kotlinx:kotlinx-collections-immutable-jvm:0.3.8")
}
project(":usvm-dataflow") {
    link("implementation", ":usvm-util")
    jaco("api", "jacodb-api-common")
    jaco("implementation", "jacodb-taint-configuration")
    lib("api", "io.github.detekt.sarif4k:sarif4k:0.5.0")
}
project(":usvm-jvm-dataflow") {
    link("api", ":usvm-dataflow"); link("implementation", ":usvm-util")
    listOf("jacodb-api-jvm", "jacodb-core", "jacodb-api-storage", "jacodb-storage", "jacodb-taint-configuration")
        .forEach { jaco("implementation", it) }
}
project(":usvm-jvm:usvm-jvm-api") { jaco("compileOnly", "jacodb-api-jvm") }
project(":usvm-jvm:usvm-jvm-test-api") {
    jaco("implementation", "jacodb-api-jvm"); link("implementation", ":usvm-jvm:usvm-jvm-api")
}
project(":usvm-jvm:usvm-jvm-util") {
    listOf("jacodb-api-jvm", "jacodb-core", "jacodb-approximations").forEach { jaco("implementation", it) }
}
project(":usvm-jvm") {
    listOf(":usvm-core", ":usvm-jvm-dataflow", ":usvm-jvm:usvm-jvm-api",
        ":usvm-jvm:usvm-jvm-test-api", ":usvm-jvm:usvm-jvm-util").forEach { link("implementation", it) }
    listOf("jacodb-api-jvm", "jacodb-core", "jacodb-approximations").forEach { jaco("implementation", it) }
    listOf("ksmt-runner", "ksmt-yices", "ksmt-cvc5", "ksmt-symfpu").forEach { lib("implementation", "io.ksmt:$it:0.5.26") }
}
