import org.gradle.api.file.DirectoryProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.bundling.Jar
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JavaToolchainService
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.register
import javax.inject.Inject
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
    id("io.papermc.paperweight.core") version "2.0.0-beta.21" apply false
}

subprojects {
    apply(plugin = "java-library")
    apply(plugin = "maven-publish")

    extensions.configure<JavaPluginExtension> {
        toolchain {
            languageVersion = JavaLanguageVersion.of(25)
        }
    }
}

val paperMavenPublicUrl = "https://repo.papermc.io/repository/maven-public/"

subprojects {
    tasks.withType<JavaCompile>().configureEach {
        options.encoding = Charsets.UTF_8.name()
        options.release = 25
        options.isFork = true
        options.compilerArgs.addAll(listOf("-Xlint:-deprecation", "-Xlint:-removal"))
    }
    tasks.withType<Javadoc>().configureEach {
        options.encoding = Charsets.UTF_8.name()
    }
    tasks.withType<ProcessResources>().configureEach {
        filteringCharset = Charsets.UTF_8.name()
    }
    tasks.withType<Test>().configureEach {
        testLogging {
            showStackTraces = true
            exceptionFormat = TestExceptionFormat.FULL
            events(TestLogEvent.STANDARD_OUT)
        }
    }

    repositories {
        mavenCentral()
        maven(paperMavenPublicUrl)
    }
}

abstract class AlkahestRunExtension @Inject constructor(objects: ObjectFactory) {
    val workingDir: DirectoryProperty = objects.directoryProperty()
    val jvmArgs: ListProperty<String> = objects.listProperty<String>()
    val serverArgs: ListProperty<String> = objects.listProperty<String>()
    val pluginJars = objects.fileCollection()
    val memoryGb: Property<Int> = objects.property<Int>()
    val nogui: Property<Boolean> = objects.property<Boolean>()
    val autoInstallTestPlugin: Property<Boolean> = objects.property<Boolean>()
}

val alkahestRun = extensions.create<AlkahestRunExtension>("alkahestRun")
val configuredRunWorkDir = providers.gradleProperty("paper.runWorkDir").orElse("run")
alkahestRun.workingDir.convention(configuredRunWorkDir.map { layout.projectDirectory.dir(it) })
alkahestRun.jvmArgs.convention(emptyList())
alkahestRun.serverArgs.convention(emptyList())
alkahestRun.memoryGb.convention(2)
alkahestRun.nogui.convention(true)

val testPluginTask = findProject(":test-plugin")
    ?.tasks
    ?.named<Jar>("jar")
val testPluginJar = testPluginTask?.flatMap { it.archiveFile }
alkahestRun.autoInstallTestPlugin.convention(testPluginJar != null)

val paperServerProject = project(":paper-server")

gradle.projectsEvaluated {
    val javaToolchains = paperServerProject.extensions.getByType<JavaToolchainService>()
    val createPaperclipJar = paperServerProject.tasks.named("createPaperclipJar")
    val paperclipJar = createPaperclipJar.map { it.outputs.files.singleFile }

    val workDir = alkahestRun.workingDir.get().asFile
    val configuredJvmArgs = alkahestRun.jvmArgs.get()
    val configuredServerArgs = alkahestRun.serverArgs.get()
    val configuredMemoryGb = alkahestRun.memoryGb.get()
    if (configuredMemoryGb <= 0) {
        throw GradleException("alkahestRun.memoryGb must be greater than zero")
    }
    val configuredNogui = alkahestRun.nogui.get()
    val configuredAutoInstallTestPlugin = alkahestRun.autoInstallTestPlugin.get()
    val configuredPluginJars = alkahestRun.pluginJars.files
    val configuredTestPlugin = if (configuredAutoInstallTestPlugin) testPluginJar?.get()?.asFile else null

    rootProject.tasks.register<JavaExec>("runAlkahest") {
        group = "runs"
        description = "Run the locally built Alkahest Paperclip jar for development"
        dependsOn(createPaperclipJar)
        dependsOn(alkahestRun.pluginJars)
        if (configuredAutoInstallTestPlugin) {
            testPluginTask?.let { dependsOn(it) }
        }
        classpath(paperclipJar)
        mainClass.set(null as String?)
        standardInput = System.`in`
        javaLauncher.set(javaToolchains.launcherFor {
            languageVersion.set(JavaLanguageVersion.of(25))
        })
        workingDir(workDir)
        minHeapSize = "${configuredMemoryGb}G"
        maxHeapSize = "${configuredMemoryGb}G"
        jvmArgs(configuredJvmArgs)
        if (configuredNogui) {
            args("--nogui")
        }
        args(configuredServerArgs)
        val pluginPaths = configuredPluginJars.map { it.absolutePath }.toMutableList()
        configuredTestPlugin?.absolutePath?.let(pluginPaths::add)
        args(pluginPaths.map { "-add-plugin=$it" })
        inputs.property("workingDir", workDir)
        inputs.property("jvmArgs", configuredJvmArgs)
        inputs.property("serverArgs", configuredServerArgs)
        inputs.property("memoryGb", configuredMemoryGb)
        inputs.property("nogui", configuredNogui)
        inputs.property("autoInstallTestPlugin", configuredAutoInstallTestPlugin)
        inputs.files(configuredPluginJars)
        testPluginJar?.let(inputs::file)
    }
}


tasks.register("printMinecraftVersion") {
    val mcVersion = providers.gradleProperty("mcVersion")
    doLast {
        println(mcVersion.get().trim())
    }
}

tasks.register("printAlkahestVersion") {
    val alkahestVersion = provider { project.version }
    doLast {
        println(alkahestVersion.get())
    }
}
