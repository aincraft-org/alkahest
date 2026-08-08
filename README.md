Alkahest — a private fork of [Paper](https://github.com/PaperMC/Paper) ("azoth", alchemical; the universal solvent).
===========

Custom gameplay lives in `dev.mintychochip.*` (seasons, ecology, genetics, custom blocks, provenance). Upstream is Paper; this fork never pushes to it.


**How To (Server Admins)**
------
Build and run the Paperclip jar:

```bash
./gradlew createPaperclipJar
java -jar paper-server/build/libs/alkahest-paperclip-*.jar nogui
```

For local development and integration testing, launch the current jar with the built-in run task:

```bash
./gradlew runAlkahest
```

This builds the current Alkahest Paperclip jar, uses `run/` as the working directory, forwards stdin, and loads the optional `test-plugin` project with Paper's `-add-plugin` option. Runtime state is not deleted automatically.

Configure the task in the root build script when a run needs different settings:

```kotlin
alkahestRun {
    workingDir.set(layout.projectDirectory.dir("run-integration"))
    memoryGb.set(4)
    jvmArgs.add("-XX:+AllowEnhancedClassRedefinition")
    serverArgs.addAll("--port", "25566")
    pluginJars.from(layout.projectDirectory.file("build/test-fixtures/example.jar"))
    nogui.set(true)
    autoInstallTestPlugin.set(false)
}
```

`runAlkahest` is the jar-oriented root launcher. The lower-level `:paper-server:runServer`, `runDevServer`, `runBundler`, and `runPaperclip` tasks remain available for Paperweight development workflows.
How To (Plugin Developers)
------
* See our API [here](paper-api)
* Paper API javadocs here: [papermc.io/javadocs](https://papermc.io/javadocs/)
#### Repository (for paper-api)
See [the docs](https://docs.papermc.io/paper/dev/project-setup/#adding-paper-as-a-dependency) for more details.
##### Gradle
```kotlin
repositories {
    maven {
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:26.2.build.+")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
}
```
##### Maven

```xml
<repository>
    <id>papermc</id>
    <url>https://repo.papermc.io/repository/maven-public/</url>
</repository>
```

```xml
<dependency>
    <groupId>io.papermc.paper</groupId>
    <artifactId>paper-api</artifactId>
    <version>[26.2.build,)</version>
    <scope>provided</scope>
</dependency>
```

How To (Compiling Jar From Source)
------
To compile Alkahest, you need JDK 25 and an internet connection.

Clone this repo, run `./gradlew applyPatches`, then `./gradlew createPaperclipJar` from your terminal. You can find the compiled jar in the `paper-server/build/libs` directory.

To get a full list of tasks, run `./gradlew tasks`.

How To (Pull Request)
------
See [Contributing](CONTRIBUTING.md)
===========