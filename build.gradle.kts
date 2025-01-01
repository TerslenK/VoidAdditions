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
    paperweight.paperDevBundle("1.21.4-R0.1-SNAPSHOT")
    implementation(libs.nova)
}

tasks.withType<JavaExec> {
    classpath = configurations.named("paperweightDevelopmentBundle").get()
}

addon {
    name = project.name.replaceFirstChar(Char::uppercase)
    version = project.version.toString()
    main = "io.github.terslenk.voidadditions.VoidAdditions" // TODO: Change this to your main class
    authors = listOf("TerslenK")
    
    // output directory for the generated addon jar is read from the "outDir" project property (-PoutDir="...")
    val outDir = project.findProperty("outDir")
    if (outDir is String)
        destination.set(File(outDir))
}

afterEvaluate {
    tasks.getByName<Jar>("jar") {
        archiveClassifier = ""
    }
}