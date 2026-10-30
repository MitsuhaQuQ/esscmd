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
    forge()
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
    get("developmentForge").extendsFrom(commonBundle)
}

repositories {
    maven("https://maven.minecraftforge.net")
}

dependencies {
    minecraft("com.mojang:minecraft:$minecraft")
    mappings(loom.officialMojangMappings())
    "forge"("net.minecraftforge:forge:$minecraft-${versionProp("deps.forge")}")

    commonBundle(project(common.path, "namedElements")) { isTransitive = false }
    shadowBundle(project(common.path, "transformProductionForge")) { isTransitive = false }
}

val runDirPath = "../../../run/$minecraft-$loader"

loom {
    forge.mixinConfigs("cmdessentials.common.mixins.json")
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
    exclude("fabric.mod.json", "architectury.common.json")
}

tasks.processResources {
    properties(
        listOf("META-INF/mods.toml", "pack.mcmeta"),
        "id" to mod.id,
        "name" to mod.name,
        "version" to mod.version,
        "description" to mod.prop("description"),
        "license" to mod.prop("license"),
        "minecraft" to versionProp("mc_dep_forge"),
        "forge_major" to versionProp("deps.forge").substringBefore('.'),
        "pack_format" to versionProp("pack_format"),
    )
}

tasks.register<Copy>("buildAndCollect") {
    group = "versioned"
    from(tasks.remapJar.get().archiveFile)
    into(rootProject.layout.buildDirectory.dir("libs/all"))
    dependsOn("build")
}
