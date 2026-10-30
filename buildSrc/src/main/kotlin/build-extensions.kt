import org.gradle.api.Project
import org.gradle.kotlin.dsl.expand
import org.gradle.language.jvm.tasks.ProcessResources

fun Project.prop(key: String): String? = findProperty(key)?.toString()
fun String.upperCaseFirst() = replaceFirstChar { it.uppercaseChar() }

val Project.mod: ModData get() = ModData(this)

/**
 * Root node of this project's version (`:fabric:1.20.1` -> `:1.20.1`).
 * It reads `versions/<mc>/gradle.properties`. Stonecutter names every node after its version.
 */
val Project.versionNode: Project get() = rootProject.project(":$name")

/** A property from `versions/<mc>/gradle.properties`. */
fun Project.versionProp(key: String): String =
    requireNotNull(versionNode.prop(key)) { "Missing '$key' in versions/${versionNode.name}/gradle.properties" }

fun ProcessResources.properties(files: Iterable<String>, vararg properties: Pair<String, Any>) {
    for ((name, value) in properties) inputs.property(name, value)
    filesMatching(files) { expand(properties.toMap()) }
}

@JvmInline
value class ModData(private val project: Project) {
    val id: String get() = prop("id")
    val name: String get() = prop("name")
    val version: String get() = prop("version")
    val group: String get() = prop("group")

    fun prop(key: String) = requireNotNull(project.prop("mod.$key")) { "Missing 'mod.$key'" }
    fun dep(key: String) = requireNotNull(project.prop("deps.$key")) { "Missing 'deps.$key'" }
}
