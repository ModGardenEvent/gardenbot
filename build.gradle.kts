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
	implementation(libs.dotenv)
	implementation(libs.gson)
	implementation(libs.logback)
    implementation(libs.jda)
	implementation(libs.sqlite)
}

tasks {
	distZip.configure {
		archiveFileName.set("gardenbot.zip")
	}
	jar.configure {
		manifest {
			attributes["Main-Class"] = "net.modgarden.gardenbot.GardenBot"
		}
		archiveFileName.set("gardenbot.jar")
	}
    withType<Zip>().configureEach {
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    }
}

distributions {
	main {
		contents {
			into("../gardenbot")
		}
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
