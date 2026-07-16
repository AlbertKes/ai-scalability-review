interface Context {
    @get:Inject
    val exec: ExecOperations
}

afterEvaluate {
    val context = project.objects.newInstance<Context>()
    val installDistDir = tasks.named<Sync>("installDist").get().destinationDir
    val projectName = project.name

    tasks.register("mongoMigrate") {
        group = "application"
        dependsOn("installDist")
        doLast {
            context.exec.exec {
                workingDir(installDistDir)
                commandLine("./bin/${projectName}")
            }
        }
    }
}
