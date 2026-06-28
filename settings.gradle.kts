pluginManagement {
    repositories { google(); mavenCentral(); gradlePluginPortal() }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories { google(); mavenCentral() }
}
rootProject.name = "ISExplorer"
include(":app")


val arch = System.getProperty("os.arch").lowercase()
val isArm = arch in setOf("arm64", "aarch64", "armv7l", "armv8l") || arch.contains("arm")

if (isArm) {
    val script = rootDir.resolve("bin/build-arm.sh")
    if (script.isFile) {
        val process = ProcessBuilder("sh", script.absolutePath)
            .directory(rootDir)
            .redirectErrorStream(true)
            .start()

        val output = process.inputStream.bufferedReader().readText()
        val code = process.waitFor()

        check(code == 0) {
            "Script failed with exit code $code\n$output"
        }
    }
}
