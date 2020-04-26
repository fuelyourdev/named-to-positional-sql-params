plugins {
    kotlin("jvm") version "1.3.72"
    id("org.jmailen.kotlinter") version "2.3.2"
    id("org.jetbrains.dokka") version "0.10.1"
    signing
    `maven-publish`
}

group = "dev.fuelyour"
version = "0.0.4"
val projectDescription = "Simple library that converts sql queries using " +
    "named parameters into sql queries using positional parameters, thus " +
    "allowing you to use named parameters even if your database tools only " +
    "support positional parameters"
description = projectDescription
extra["isReleaseVersion"] = !version.toString().endsWith("SNAPSHOT")

buildscript {
    repositories {
        jcenter()
        mavenCentral()
    }
}

repositories {
    jcenter()
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))

    testImplementation("io.kotest:kotest-runner-junit5:4.0.3")
}

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
    test {
        useJUnitPlatform()
        testLogging {
            events("passed", "skipped", "failed")
            showStandardStreams = true
        }
    }
    dokka {
        outputFormat = "html"
        outputDirectory = "$buildDir/dokka"
    }
}

val sourcesJar by tasks.creating(org.gradle.jvm.tasks.Jar::class) {
    dependsOn(JavaPlugin.CLASSES_TASK_NAME)
    archiveClassifier.set("sources")
    from(sourceSets["main"].allSource)
}

val dokkaJar by tasks.creating(org.gradle.jvm.tasks.Jar::class) {
    group = JavaBasePlugin.DOCUMENTATION_GROUP
    description = "Assembles Kotlin docs with Dokka"
    archiveClassifier.set("javadoc")
    from(tasks.dokka)
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            artifact(sourcesJar)
            artifact(dokkaJar)
            pom {
                name.set("${project.group}:${rootProject.name}")
                description.set(projectDescription)
                url.set("https://github.com/fuelyourdev/${rootProject.name}")
                licenses {
                    license {
                        name.set("MIT License")
                        url.set(
                            "http://www.opensource.org/licenses/mit-license.php"
                        )
                    }
                }
                developers {
                    developer {
                        id.set("fuelyourdev")
                        name.set("Trevor Young")
                        email.set("trevor@fuelyour.dev")
                        organization.set("Fuel Your Dev, LLC")
                        organizationUrl.set("https://fuelyour.dev")
                    }
                }
                scm {
                    connection.set("scm:git:git://github.com/fuelyourdev/${rootProject.name}.git")
                    developerConnection.set(
                        "scm:git:git://github.com/fuelyourdev/${rootProject.name}.git"
                    )
                    url.set("https://github.com/fuelyourdev/${rootProject.name}/tree/master")
                }
            }
        }
    }
    repositories {
        maven {
            val repoUrl = if (rootProject.extra["isReleaseVersion"] as Boolean) {
                "https://oss.sonatype.org/service/local/staging/deploy/maven2"
            } else {
                "https://oss.sonatype.org/content/repositories/snapshots"
            }
            url = uri(repoUrl)
            credentials {
                username = System.getenv("ossrhUsername")
                password = System.getenv("ossrhPassword")
            }
        }
    }
}

if (rootProject.extra["isReleaseVersion"] as Boolean) {
    signing {
        val signingKey: String? by project
        val signingPassword: String? by project
        useInMemoryPgpKeys(signingKey, signingPassword)
        sign(publishing.publications["mavenJava"])
    }
}