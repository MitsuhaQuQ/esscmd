plugins {
    id("dev.architectury.loom")
    id("architectury-plugin")
}

val minecraft = stonecutter.current.version

version = "${mod.version}+$minecraft"
base {
    archivesName.set("${mod.id}-common")
}

architectury {
    common("fabric", "forge")
}

dependencies {
    minecraft("com.mojang:minecraft:$minecraft")
    mappings(loom.officialMojangMappings())
    // Only for @Environment/mixin annotations on the compile classpath; never call Fabric API from common.
    modImplementation("net.fabricmc:fabric-loader:${mod.dep("fabric_loader")}")
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(versionProp("java"))
}
