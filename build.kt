dependencies {
    paperweight.paperDevBundle("1.21.4-R0.1-SNAPSHOT")
}
tasks.withType<JavaExec> {
    classpath = configurations.named("paperweightDevelopmentBundle").get().files.singleFile
}