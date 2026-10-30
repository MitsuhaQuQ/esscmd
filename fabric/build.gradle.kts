plugins {
    id("dev.architectury.loom")
    id("architectury-plugin")
    id("com.github.johnrengelman.shadow")
}

val loader = prop("loom.platform")!!
val minecraft = stonecutter.current.version
val common: Project = requireNotNull(stonecutter.node.sibling("common")?.project) {
    "No common project for $path"
}

version = "${mod.version}+$minecraft"
base {
    archivesName.set("${mod.id}-$loader")
}

architectury {
    platformSetupLoomIde()
    fabric()
}

val commonBundle: Configuration by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
}
val shadowBundle: Configuration by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
}

configurations {
    compileClasspath.get().extendsFrom(commonBundle)
    runtimeClasspath.get().extendsFrom(commonBundle)
    get("developmentFabric").extendsFrom(commonBundle)
}

dependencies {
    minecraft("com.mojang:minecraft:$minecraft")
    mappings(loom.officialMojangMappings())
    modImplementation("net.fabricmc:fabric-loader:${mod.dep("fabric_loader")}")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${versionProp("deps.fabric_api")}")
    // Soft dependency: permission plugins such as LuckPerms ship this API themselves.
    modCompileOnly("me.lucko:fabric-permissions-api:${mod.dep("fabric_permissions_api")}") { isTransitive = false }

    commonBundle(project(common.path, "namedElements")) { isTransitive = false }
    shadowBundle(project(common.path, "transformProductionFabric")) { isTransitive = false }
}

val runDirPath = "../../../run/$minecraft-$loader"

loom {
    runConfigs.all {
        isIdeConfigGenerated = true
        runDir = runDirPath
    }
    runConfigs.remove(runConfigs["client"])
    runConfigs["server"].programArgs("nogui")
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(versionProp("java"))
}

tasks.withType<net.fabricmc.loom.task.AbstractRunTask>().configureEach {
    javaLauncher = javaToolchains.launcherFor(java.toolchain)
    // Dev servers only: accept the EULA so the server doesn't stop at first start.
    val eula = file(runDirPath).resolve("eula.txt")
    doFirst { eula.apply { parentFile.mkdirs() }.writeText("eula=true\n") }
}

tasks.jar {
    archiveClassifier = "dev"
}

tasks.remapJar {
    inputFile = tasks.shadowJar.get().archiveFile
    archiveClassifier = null
    dependsOn(tasks.shadowJar)
}

tasks.shadowJar {
    configurations = listOf(shadowBundle)
    archiveClassifier = "dev-shadow"
    exclude("architectury.common.json")
}

tasks.processResources {
    properties(
        listOf("fabric.mod.json"),
        "id" to mod.id,
        "name" to mod.name,
        "version" to mod.version,
        "description" to mod.prop("description"),
        "license" to mod.prop("license"),
        "minecraft" to versionProp("mc_dep_fabric"),
        "java" to versionProp("java"),
    )
}

tasks.register<Copy>("buildAndCollect") {
    group = "versioned"
    from(tasks.remapJar.get().archiveFile)
    into(rootProject.layout.buildDirectory.dir("libs/all"))
    dependsOn("build")
}
