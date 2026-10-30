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

    testImplementation(platform("org.junit:junit-bom:${mod.dep("junit")}"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

// Unit tests cover pure-Java helpers only (no Minecraft bootstrap), so running them on the
// active version is enough: `./gradlew :common:1.20.1:test`.
tasks.test {
    useJUnitPlatform()
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(versionProp("java"))
}
