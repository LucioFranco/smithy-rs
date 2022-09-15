/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

import org.gradle.api.tasks.testing.logging.TestExceptionFormat

plugins {
    kotlin("jvm")
    id("org.jetbrains.dokka")
    jacoco
    `maven-publish`
}

description = "Generates Rust client code from Smithy models"
extra["displayName"] = "Smithy :: Rust :: CodegenClient"
extra["moduleName"] = "software.amazon.smithy.rust.codegen.client"

group = "software.amazon.smithy.rust.codegen"
version = "0.1.0"

val smithyVersion: String by project
val kotestVersion: String by project

dependencies {
    implementation(project(":codegen-core"))
    implementation(kotlin("stdlib-jdk8"))
    implementation("org.jsoup:jsoup:1.14.3")
    api("software.amazon.smithy:smithy-codegen-core:$smithyVersion")
    api("com.moandjiezana.toml:toml4j:0.7.2")
    implementation("software.amazon.smithy:smithy-aws-traits:$smithyVersion")
    implementation("software.amazon.smithy:smithy-protocol-test-traits:$smithyVersion")
    implementation("software.amazon.smithy:smithy-waiters:$smithyVersion")
    implementation("software.amazon.smithy:smithy-rules-engine:$smithyVersion") {
        version {
            branch = "rules-engine"
        }
    }
    runtimeOnly(project(":rust-runtime"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.6.1")
    testImplementation("io.kotest:kotest-assertions-core-jvm:$kotestVersion")
}

tasks.compileTestKotlin {
    kotlinOptions.jvmTarget = "1.8"
}

tasks.compileKotlin {
    kotlinOptions.jvmTarget = "1.8"
}

// Reusable license copySpec
val licenseSpec = copySpec {
    from("${project.rootDir}/LICENSE")
    from("${project.rootDir}/NOTICE")
}

// Configure jars to include license related info
tasks.jar {
    metaInf.with(licenseSpec)
    inputs.property("moduleName", project.name)
    manifest {
        attributes["Automatic-Module-Name"] = project.name
    }
}

val sourcesJar by tasks.creating(Jar::class) {
    group = "publishing"
    description = "Assembles Kotlin sources jar"
    classifier = "sources"
    from(sourceSets.getByName("main").allSource)
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
        exceptionFormat = TestExceptionFormat.FULL
        showCauses = true
        showExceptions = true
        showStackTraces = true
        showStandardStreams = true
    }
}

tasks.dokkaHtml.configure {
    outputDirectory.set(buildDir.resolve("javadoc"))
}

// Always build documentation
tasks["build"].finalizedBy(tasks["dokkaHtml"])

// Configure jacoco (code coverage) to generate an HTML report
tasks.jacocoTestReport {
    reports {
        xml.isEnabled = false
        csv.isEnabled = false
        html.destination = file("$buildDir/reports/jacoco")
    }
}

// Always run the jacoco test report after testing.
tasks["test"].finalizedBy(tasks["jacocoTestReport"])

publishing {
    publications {
        create<MavenPublication>("default") {
            from(components["java"])
            artifact(sourcesJar)
        }
    }
    repositories { maven { url = uri("$buildDir/repository") } }
}
