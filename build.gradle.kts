apply(plugin = "project")
apply(plugin = "azure-maven")

plugins {
    `java-library`
}

subprojects {
    group = "com.foodtruck"
    version = "1.0.0"

    repositories {
        maven {
            url = uri("https://neowu.github.io/maven-repo/")
            content {
                includeGroupByRegex("core\\.framework.*")
            }
        }
        mavenCentral()
    }

    if (!plugins.hasPlugin("java")) {
        return@subprojects
    }

    dependencies {
        implementation(platform("com.wonder:wonder-dependencies:3.0.+"))
    }
}

configure(subprojects.filter { it.name.endsWith("-interface") }) {
    apply(plugin = "lib")
    dependencies {
        implementation("com.wonder:core-ng-api")
    }
    tasks.withType<JavaCompile> {
        options.release = 25
    }
}

configure(subprojects.filter {
    it.name.endsWith("-service") || it.name.endsWith("-api")
}) {
    apply(plugin = "app")
    dependencies {
        implementation("com.wonder:core-ng")
        testImplementation("com.wonder:core-ng-test")
    }
}

configure(subprojects.filter { it.name.endsWith("-mongo-migration") }) {
    apply(plugin = "app")
    apply(plugin = "mongo-migration")
    dependencies {
        implementation("com.wonder:core-ng")
        implementation("com.wonder:core-ng-mongo")
        implementation("com.wonder:core-ext-mongo-migration")
    }
}

// services use mongodb
configure(
    listOf(
        project(":ai-scalability-review-service")
    )
) {
    dependencies {
        implementation("com.wonder:core-ng-mongo")
        testImplementation("com.wonder:core-ng-mongo-test")
    }
}

project("ai-scalability-review-service") {
    dependencies {
        implementation(project(":ai-scalability-review-service-interface"))
    }
}
