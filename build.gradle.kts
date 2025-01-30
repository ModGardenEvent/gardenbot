import org.jetbrains.gradle.ext.Application
import org.jetbrains.gradle.ext.runConfigurations
import org.jetbrains.gradle.ext.settings

plugins {
    application
    java
    idea
    `java-library-distribution`
    alias(libs.plugins.idea.ext) apply true
}

group = "net.modgarden"
version = project.properties["version"].toString()

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
    withJavadocJar()
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.jda)
}

tasks {
    val expandProps = mapOf(
        "version" to version
    )

    val processResourcesTasks = listOf("processResources", "processTestResources")

    assemble.configure {
        dependsOn(processResourcesTasks)
    }

    jar.configure {
        manifest {
            attributes["Main-Class"] = "net.modgarden.backend.ModGardenBackend"
        }
    }
    withType<ProcessResources>().matching { processResourcesTasks.contains(it.name) }.configureEach {
        inputs.properties(expandProps)
        filesMatching("landing.json") {
            expand(expandProps)
        }
    }
    withType<Zip>().configureEach {
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    }
}

distributions {
    main {
        distributionBaseName = "gardenbot"
    }
}

application {
    mainClass = "net.modgarden.gardenbot.GardenBot"
}

idea {
    project {
        settings.runConfigurations {
            create("Run", Application::class.java) {
                workingDirectory = "${rootProject.projectDir}/run"
                mainClass = "net.modgarden.gardenbot.GardenBot"
                moduleName = project.idea.module.name + ".main"
                includeProvidedDependencies = true
                envs = mapOf(
                    "env" to "development"
                )
            }
        }
    }
}
