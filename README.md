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

The optional `test-plugin` has a compile-time dependency on `dev.craftux:craftux-paper`. The current Paper server launch tasks add only the locally built `test-plugin` jar with `-add-plugin`; they do not resolve or load a CraftUX jar automatically. To use CraftUX at runtime, pass its jar explicitly with Paper's `-add-plugin` option.

CraftUX is published to GitHub Packages, so configure a token with package read access before building `test-plugin`:

```bash
export GITHUB_ACTOR=your-github-user
export GITHUB_TOKEN=your-github-pat
./gradlew :test-plugin:jar
```

Override the dependency with `-PcraftuxVersion=1.0.3`; Gradle properties `craftuxRepoUser` and `craftuxRepoToken` (or `gpr.user` / `gpr.key`) are also supported. The lower-level `:paper-server:runServer`, `runDevServer`, `runBundler`, and `runPaperclip` tasks remain available for Paperweight development workflows.
How To (Plugin Developers)
------
* API source: [`alkahest-api`](alkahest-api)
* Downloadable API jars: [GitHub releases](https://github.com/aincraft-org/alkahest/releases/latest)
* Paper API javadocs: [papermc.io/javadocs](https://papermc.io/javadocs/)
#### API artifact
Release assets are named `alkahest-api-YYYY.MM.DD.N.jar`.

To build the API from source and install it into the local Maven repository:

```bash
./gradlew :alkahest-api:publishToMavenLocal -PalkahestVersion=2026.08.08.3
```

##### Gradle

```kotlin
repositories {
    mavenLocal()
}

dependencies {
    compileOnly("io.papermc.paper:alkahest-api:2026.08.08.3")
}
```

##### Maven

```xml
<repository>
    <id>local</id>
    <url>file://${user.home}/.m2/repository</url>
</repository>
```

```xml
<dependency>
    <groupId>io.papermc.paper</groupId>
    <artifactId>alkahest-api</artifactId>
    <version>2026.08.08.3</version>
    <scope>provided</scope>
</dependency>
```

The API keeps the `org.bukkit`, `io.papermc.paper`, and `dev.mintychochip`
Java packages. The artifact coordinate is intentionally Alkahest-specific.

```kotlin
java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
}
```

How To (Compiling Jar From Source)
------
To compile Alkahest, you need JDK 25 and an internet connection.

Clone this repo, run `./gradlew applyPatches`, then build either the API with
`./gradlew :alkahest-api:jar` or the server with
`./gradlew createPaperclipJar`. API jars are written to
`alkahest-api/build/libs`; server jars are written to `paper-server/build/libs`.

To get a full list of tasks, run `./gradlew tasks`.

How To (Pull Request)
------
See [Contributing](CONTRIBUTING.md)
===========