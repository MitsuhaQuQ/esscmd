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
    exclusiveContent {
        forRepository { maven("https://api.modrinth.com/maven") }
        filter { includeGroup("maven.modrinth") }
    }
}

dependencies {
    minecraft("com.mojang:minecraft:$minecraft")
    implementation("net.fabricmc:fabric-loader:${versionProp("deps.fabric_loader")}")
    implementation("net.fabricmc.fabric-api:fabric-api:${versionProp("deps.fabric_api")}")
    // Soft dependency: permission plugins such as LuckPerms ship this API themselves.
    compileOnly("maven.modrinth:fabric-permissions-api:${versionProp("deps.fabric_permissions_api")}") { isTransitive = false }
    implementation(project(common.path)) { isTransitive = false }
}

val runDirPath = "../../../run/$minecraft-$loader"

loom {
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
    from(tasks.jar.get().archiveFile)
    into(rootProject.layout.buildDirectory.dir("libs/all"))
    dependsOn("build")
}
