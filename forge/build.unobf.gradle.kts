// Build script for Minecraft 26.1+: `common` is compiled as it is and copied into the jar.
plugins {
    id("dev.architectury.loom-no-remap")
}

val loader = prop("loom.platform")!!
val minecraft = stonecutter.current.version
val common: Project = requireNotNull(stonecutter.node.sibling("common")?.project) {
    "No common project for $path"
}
evaluationDependsOn(common.path)
val commonMain = common.the<SourceSetContainer>()["main"]

version = "${mod.version}+$minecraft"
base {
    archivesName.set("${mod.id}-$loader")
}

repositories {
    maven("https://maven.minecraftforge.net")
}

dependencies {
    minecraft("com.mojang:minecraft:$minecraft")
    "forge"("net.minecraftforge:forge:$minecraft-${versionProp("deps.forge")}")
    implementation(project(common.path)) { isTransitive = false }
}

val runDirPath = "../../../run/$minecraft-$loader"

loom {
    forge.mixinConfigs("cmdessentials.common.mixins.json")
    mods {
        register(mod.id) {
            sourceSet(sourceSets.main.get())
            sourceSet(commonMain)
        }
    }
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
    from(commonMain.output)
    exclude("fabric.mod.json")
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
    from(tasks.jar.get().archiveFile)
    into(rootProject.layout.buildDirectory.dir("libs/all"))
    dependsOn("build")
}
