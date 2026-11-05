plugins {
    id("dev.kikugie.stonecutter")
    id("dev.architectury.loom") version "1.17.493" apply false
    id("dev.architectury.loom-no-remap") version "1.17.493" apply false
    id("architectury-plugin") version "3.5.170" apply false
    id("com.gradleup.shadow") version "9.6.1" apply false
}
stonecutter active "1.20.1" /* [SC] DO NOT EDIT */

// Vanilla classes Mojang renamed; this keeps command code free of `//?` blocks.
stonecutter parameters {
    replacement(eval(metadata.version, ">=1.21.11"), "ResourceLocation", "Identifier")
    replacement(eval(metadata.version, ">=26.1"), "ClickType", "ContainerInput")
}

// Builds every loader jar for every version into `build/libs/all/`.
stonecutter registerChiseled tasks.register("buildAndCollect", stonecutter.chiseled) {
    group = "project"
    versions { branch, _ -> branch == "fabric" || branch == "forge" }
    ofTask("buildAndCollect")
}

// Dev servers for the active version: `runActiveServerFabric`, `runActiveServerForge`.
for (node in stonecutter.tree.nodes) {
    if (node.metadata != stonecutter.current || node.branch.id !in setOf("fabric", "forge")) continue
    tasks.register("runActiveServer${node.branch.id.upperCaseFirst()}") {
        group = "project"
        dependsOn("${node.hierarchy}:runServer")
    }
}
