group = "io.github.terslenk.voidadditions" // TODO: Change this to your group
version = "0.1-dev" // TODO: Change this to your addon version

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
    name.set(project.name)
    version.set(project.version.toString())
    main.set("io.github.terslenk.voidadditions.VoidAdditions") // TODO: Change this to your main class
    authors.set(listOf("TerslenK"))
    
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