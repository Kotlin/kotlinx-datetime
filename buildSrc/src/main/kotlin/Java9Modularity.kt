import org.gradle.api.*
import org.gradle.api.file.*
import org.gradle.api.tasks.bundling.*
import org.gradle.api.tasks.compile.*
import org.gradle.kotlin.dsl.*
import org.gradle.util.GUtil.*
import org.jetbrains.kotlin.gradle.dsl.*
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.*
import org.jetbrains.kotlin.gradle.targets.jvm.*
import java.io.*

object Java9Modularity {

    @JvmStatic
    @JvmOverloads
    fun configureJava9ModuleInfo(project: Project, multiRelease: Boolean = true) {
        val kotlin = project.extensions.findByType<KotlinProjectExtension>() ?: return
        project.configureModuleInfoForKotlinProject(kotlin, multiRelease)
    }

    private fun Project.configureModuleInfoForKotlinProject(kotlin: KotlinProjectExtension, multiRelease: Boolean = true) {
        val jvmTargets = kotlin.targets.filter { it is KotlinJvmTarget || it is KotlinWithJavaTarget<*> }
        if (jvmTargets.isEmpty()) {
            logger.warn("No Kotlin JVM targets found, can't configure compilation of module-info!")
        }
        jvmTargets.forEach { target ->
            target.compilations.forEach { compilation ->
                configureModuleInfoForKotlinCompilation(compilation)
            }

            if (multiRelease) {
                tasks.getByName<Jar>(target.artifactsTaskName) {
                    rename("module-info.class", "META-INF/versions/9/module-info.class")
                    manifest {
                        attributes("Multi-Release" to true)
                    }
                }
            }
        }
    }

    private fun Project.configureModuleInfoForKotlinCompilation(compilation: KotlinCompilation<*>) {
        val defaultSourceSet = compilation.defaultSourceSet.kotlin
        val moduleInfoSourceFile = defaultSourceSet.find { it.name == "module-info.java" }

        if (moduleInfoSourceFile == null) {
            logger.info("No module-info.java file found in ${defaultSourceSet.srcDirs}, can't configure compilation of module-info!")
        } else {
            val targetName = toCamelCase(compilation.target.targetName)
            val compilationName = if (compilation.name != KotlinCompilation.MAIN_COMPILATION_NAME) toCamelCase(compilation.name) else ""
            val compileModuleInfoTaskName = "compile${compilationName}ModuleInfo$targetName"

            val compileKotlinTask = compilation.compileKotlinTask as AbstractCompile
            val modulePath = compileKotlinTask.classpath

            val compileModuleInfoTask = registerCompileModuleInfoTask(compileModuleInfoTaskName, modulePath, compileKotlinTask.destinationDirectory, moduleInfoSourceFile)
            tasks.getByName(compilation.compileAllTaskName).dependsOn(compileModuleInfoTask)
        }
    }

    private fun Project.registerCompileModuleInfoTask(taskName: String, modulePath: FileCollection, destinationDir: DirectoryProperty, moduleInfoSourceFile: File) =
        tasks.register(taskName, JavaCompile::class) {
            dependsOn(modulePath)
            source(moduleInfoSourceFile)
            classpath = files()
            destinationDirectory.set(destinationDir)
            sourceCompatibility = JavaVersion.VERSION_1_9.toString()
            targetCompatibility = JavaVersion.VERSION_1_9.toString()
            doFirst {
                options.compilerArgs = listOf(
                    "--release", "9",
                    "--module-path", modulePath.asPath,
                    "-Xlint:-requires-transitive-automatic"
                )
            }
        }
}
