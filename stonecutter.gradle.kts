plugins {
    id("dev.kikugie.stonecutter")
    id("dev.architectury.loom") version "1.9.436" apply false
    id("architectury-plugin") version "3.4.164" apply false
    id("com.github.johnrengelman.shadow") version "8.1.1" apply false
}
stonecutter active "1.20.1" /* [SC] DO NOT EDIT */

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
