import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

group = "io.github.terslenk.voidadditions" // TODO: Change this to your group
version = "0.0 - DEVELOPMENT BUILD" // TODO: Change this to your addon version

plugins {
    alias(libs.plugins.kotlin)
    alias(libs.plugins.paperweight)
    alias(libs.plugins.nova)
}

repositories {
    mavenCentral()
    maven("https://papermc.io/repo/repository/maven-public/")
    maven("https://repo.xenondevs.xyz/releases")
}

dependencies {
    paperweight.paperDevBundle(libs.versions.paper)
    implementation(libs.nova)
}

addon {
    id = project.name
    name = project.name.replaceFirstChar(Char::uppercase).replace("_a" ," A")
    version = project.version.toString()
    novaVersion = libs.versions.nova
    main = "io.github.terslenk.voidadditions.VoidAdditions" // TODO: Change this to your main class
    authors = listOf("TerslenK") // TODO: Set your list of authors
}

tasks {
    register<Copy>("addonJar") {
        group = "build"
        dependsOn("jar")
        from(File(project.layout.buildDirectory.get().asFile, "libs/${project.name}-${project.version}.jar"))
        into((project.findProperty("outDir") as? String)?.let(::File) ?: project.layout.buildDirectory.get().asFile)
        rename { "${addonMetadata.get().addonName.get()} - v${project.version}.jar" }
    }
    
    withType<KotlinCompile> {
        compilerOptions { 
            jvmTarget = JvmTarget.JVM_21
        }
    }
}

afterEvaluate {
    tasks.getByName<Jar>("jar") {
        archiveClassifier = ""
    }
}