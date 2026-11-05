// Build script for Minecraft 26.1+, which ships unobfuscated: no mappings and no remapping.
plugins {
    id("dev.architectury.loom-no-remap")
}

val minecraft = stonecutter.current.version

version = "${mod.version}+$minecraft"
base {
    archivesName.set("${mod.id}-common")
}

dependencies {
    minecraft("com.mojang:minecraft:$minecraft")
    // Only for mixin annotations on the compile classpath; never call Fabric API from common.
    compileOnly("net.fabricmc:fabric-loader:${versionProp("deps.fabric_loader")}")
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(versionProp("java"))
}
