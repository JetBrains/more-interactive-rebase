import com.diffplug.spotless.LineEnding
import org.jetbrains.changelog.Changelog
import org.jetbrains.changelog.markdownToHTML

fun properties(key: String) = providers.gradleProperty(key)
fun environment(key: String) = providers.environmentVariable(key)

buildscript {
    repositories {
        mavenCentral()
        //Needed only for SNAPSHOT versions
        //maven { url 'https://oss.sonatype.org/content/repositories/snapshots/' }
    }
    dependencies {
        classpath("info.solidsoft.gradle.pitest:gradle-pitest-plugin:1.15.0")
    }
}

plugins {
    id("java")
    alias(libs.plugins.kotlin)
    alias(libs.plugins.gradleIntelliJPlugin)
    alias(libs.plugins.changelog)
    alias(libs.plugins.qodana)
    alias(libs.plugins.kover)
    id("com.diffplug.spotless") version "6.25.0"
    id("jacoco")
    id("pmd")
    id("info.solidsoft.pitest") version "1.15.0"
}

group = properties("pluginGroup").get()
version = properties("pluginVersion").get()

repositories {
    mavenCentral()
    gradlePluginPortal()

    maven ("https://oss.sonatype.org/content/repositories/snapshots/" )
    maven("https://www.jetbrains.com/intellij-repository/releases/")
    maven("https://cache-redirector.jetbrains.com/intellij-dependencies")
    maven("https://cache-redirector.jetbrains.com/download.jetbrains.com/teamcity-repository")
    maven("https://cache-redirector.jetbrains.com/download-pgp-verifier")
    maven("https://cache-redirector.jetbrains.com/packages.jetbrains.team/maven/p/grazi/grazie-platform-public")
    maven("https://maven.pkg.jetbrains.space/public/p/ktor/eap/")
}

// Dependencies are managed with Gradle version catalog - read more: https://docs.gradle.org/current/userguide/platforms.html#sub:version-catalog
dependencies {
    implementation(libs.annotations)

    testImplementation("org.junit.jupiter:junit-jupiter:5.8.2")
    testImplementation("org.mockito:mockito-core:3.12.4")
    testImplementation("org.assertj:assertj-core:3.23.1")
    testImplementation("org.assertj:assertj-swing-junit:3.9.2")
    testImplementation("junit:junit:4.12")
//    testImplementation("com.jetbrains.intellij.platform:vcs-test-framework:241.15989.150")
//    testImplementation("com.jetbrains.intellij.platform:test-framework:241.15989.150")
//    testImplementation("org.jetbrains.kotlin:kotlin-test:2.0.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
    testImplementation("org.awaitility:awaitility:4.2.0")
}

kotlin {
    jvmToolchain(17)
}

// Configure Gradle IntelliJ Plugin - read more: https://plugins.jetbrains.com/docs/intellij/tools-gradle-intellij-plugin.html
intellij {
    pluginName = properties("pluginName")
    version = properties("platformVersion")
    type = properties("platformType")
    instrumentCode = false
    // Plugin Dependencies. Uses `platformPlugins` property from the gradle.properties file.
    plugins = properties("platformPlugins").map { it.split(',').map(String::trim).filter(String::isNotEmpty) }
}

// Configure Gradle Changelog Plugin - read more: https://github.com/JetBrains/gradle-changelog-plugin
changelog {
    groups.empty()
    repositoryUrl = properties("pluginRepositoryUrl")
}

jacoco {
    toolVersion = "0.8.9" // Use the desired version of JaCoCo
//    reportsDirectory = layout.buildDirectory.dir("reports/jacoco")
}

pmd {
    isConsoleOutput = true
    toolVersion = "6.21.0"
    incrementalAnalysis = true
}

// Configure Gradle Kover Plugin - read more: https://github.com/Kotlin/kotlinx-kover#configuration
koverReport {
    defaults {
        xml {
            onCheck = true
        }
    }
}

configure<com.diffplug.gradle.spotless.SpotlessExtension> {
    kotlin {
        // by default the target is every '.kt' and '.kts` file in the java sourcesets
        ktfmt().dropboxStyle()
        ktlint()
        lineEndings = LineEnding.UNIX
        //diktat()
        //prettier()
    }
}

apply(plugin = "info.solidsoft.pitest")

val integrationTests: SourceSet by sourceSets.creating {
    kotlin.srcDir("src/integrationTests/kotlin")
    resources.srcDir("src/integrationTests/resources")
    compileClasspath += sourceSets["main"].output + sourceSets["test"].output
    runtimeClasspath += output + compileClasspath
}

configurations {
    getByName("integrationTestsImplementation") {
        extendsFrom(configurations["testImplementation"])
    }
    getByName("integrationTestsRuntimeOnly") {
        extendsFrom(configurations["testRuntimeOnly"])
    }
}
dependencies {
    "integrationTestsImplementation"("com.jetbrains.intellij.platform:vcs-test-framework:241.15989.150")
    "integrationTestsImplementation"("com.jetbrains.intellij.platform:test-framework:241.15989.150")
}

val integrationTestTask = tasks.register<Test>("integrationTest") {
    description = "Runs the integration tests."
    group = "verification"
    testClassesDirs = integrationTests.output.classesDirs
    classpath = integrationTests.runtimeClasspath
//    shouldRunAfter(tasks.named("test"))
    useJUnit()
    maxParallelForks = 1
    systemProperty("junit.jupiter.execution.parallel.enabled", "false")
    systemProperties["idea.home.path"] = System.getProperty("java.io.tmpdir")
}

tasks {
    wrapper {
        gradleVersion = properties("gradleVersion").get()
    }

    patchPluginXml {
        version = properties("pluginVersion")
        sinceBuild = properties("pluginSinceBuild")
        untilBuild = properties("pluginUntilBuild")

        // Extract the <!-- Plugin description --> section from README.md and provide for the plugin's manifest
        pluginDescription = providers.fileContents(layout.projectDirectory.file("README.md")).asText.map {
            val start = "<!-- Plugin description -->"
            val end = "<!-- Plugin description end -->"

            with (it.lines()) {
                if (!containsAll(listOf(start, end))) {
                    throw GradleException("Plugin description section not found in README.md:\n$start ... $end")
                }
                subList(indexOf(start) + 1, indexOf(end)).joinToString("\n").let(::markdownToHTML)
            }
        }

        val changelog = project.changelog // local variable for configuration cache compatibility
        // Get the latest available change notes from the changelog file
        changeNotes = properties("pluginVersion").map { pluginVersion ->
            with(changelog) {
                renderItem(
                    (getOrNull(pluginVersion) ?: getUnreleased())
                        .withHeader(false)
                        .withEmptySections(false),
                    Changelog.OutputType.HTML,
                )
            }
        }
    }

//    // Configure UI tests plugin
//    // Read more: https://github.com/JetBrains/intellij-ui-test-robot
//    runIdeForUiTests {
//        systemProperty("robot-server.port", "8082")
//        systemProperty("ide.mac.message.dialogs.as.sheets", "false")
//        systemProperty("jb.privacy.policy.text", "<!--999.999-->")
//        systemProperty("jb.consents.confirmation.enabled", "false")
//    }

    signPlugin {
        certificateChain = environment("CERTIFICATE_CHAIN")
        privateKey = environment("PRIVATE_KEY")
        password = environment("PRIVATE_KEY_PASSWORD")
    }

    publishPlugin {
        dependsOn("patchChangelog")
        token.set(System.getenv("ORG_GRADLE_PROJECT_intellijPublishToken"))
        // The pluginVersion is based on the SemVer (https://semver.org) and supports pre-release labels, like 2.1.7-alpha.3
        // Specify pre-release label to publish the plugin in a custom Release Channel automatically. Read more:
        // https://plugins.jetbrains.com/docs/intellij/deployment.html#specifying-a-release-channel
        channels = properties("pluginVersion").map { listOf(it.substringAfter('-', "").substringBefore('.').ifEmpty { "default" }) }
    }

    compileKotlin {
        kotlinOptions {
            jvmTarget = "17"
        }
    }

    compileTestKotlin {
        kotlinOptions {
            jvmTarget = "17"
        }
    }

    test {
        useJUnit()
        jacoco {
            enabled = true
            finalizedBy(jacocoTestCoverageVerification)

        }

        pitest {
            targetClasses.set(setOf("com.jetbrains.interactiveRebase.*")) //by default "${project.group}.*"
            pitestVersion.set("1.15.0") //not needed when a default PIT version should be used
            threads.set(4)
            outputFormats.set(setOf("XML", "HTML"))
            timestampedReports.set(false)
        }
    }

    jacocoTestReport {
        dependsOn(test)


    }
    jacocoTestCoverageVerification {
        dependsOn(test)
        violationRules {
            rule {
                enabled = true
                element = "CLASS"
                includes =  listOf("com.jetbrains.interactiveRebase.**")
                excludes = listOf("git4ideaClasses.**")


                limit {
                    counter = "BRANCH"
                    value = "COVEREDRATIO"
                    minimum = "0.0".toBigDecimal()
                }
            }
        }
    }
}

tasks.withType(Test::class) {
    configure<JacocoTaskExtension> {
        isIncludeNoLocationClasses = true
        includes = listOf("com.jetbrains.interactiveRebase.**")
        excludes = listOf("git4ideaClasses.**")


    }

    tasks.withType<JacocoCoverageVerification> {
        violationRules {
            rule {
                limit {
                    minimum = BigDecimal(0.62)
                }
            }
        }

        afterEvaluate {
            classDirectories.setFrom(files(classDirectories.files.map {
                fileTree(it).apply {
                    exclude("git4ideaClasses/**")
                }
            }))
        }
    }

    tasks.withType<JacocoReport> {
        afterEvaluate {
            classDirectories.setFrom(files(classDirectories.files.map {
                fileTree(it).apply {
                    exclude("git4ideaClasses/**")
                }
            }))
        }
    }
}

tasks.named("check") {
    dependsOn(integrationTestTask)
}

