import org.gradle.api.Project
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.compile.AbstractCompile
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.kotlin.dsl.register
import java.util.spi.ToolProvider

object Java9Modularity {
    @JvmStatic
    fun configureJava9ModuleInfo(project: Project) {
        project.afterEvaluate {
            val jdeps = ToolProvider.findFirst("jdeps").orElseThrow { IllegalStateException("Tool 'jdeps' is not available") }
            val compileKotlinJvm = tasks.findByName("compileKotlinJvm") as AbstractCompile?
            val jvmJar = tasks.findByName("jvmJar") as Jar?

            if (compileKotlinJvm != null && jvmJar != null) {
                val modularityDir = buildDir.resolve("modularity")
                val generatedDir = modularityDir.resolve("generated")
                val classesDir = modularityDir.resolve("classes")
                val jvmGenerateModuleInfo = tasks.register("jvmGenerateModuleInfo", Jar::class) {
                    dependsOn(compileKotlinJvm)
                    destinationDirectory.set(modularityDir)
                    from(jvmJar.source)
                    exclude { it.file.startsWith(classesDir) }
                    doLast {
                        generatedDir.deleteRecursively()
                        jdeps.run(
                            System.out, System.err,
                            "--multi-release", "9",
                            "--generate-module-info", generatedDir.toString(),
                            "--module-path", compileKotlinJvm.classpath.asPath,
                            archiveFile.get().toString()
                        )
                    }
                }
                val jvmCompileModuleInfo = tasks.register("jvmCompileModuleInfo", JavaCompile::class) {
                    dependsOn(jvmGenerateModuleInfo)
                    destinationDirectory.set(classesDir)
                    source(generatedDir)
                    classpath = files()
                    doFirst {
                        val moduleName = generatedDir.listFiles().first().name
                        options.compilerArgs = listOf(
                            "--release", "9",
                            "--module-path", compileKotlinJvm.classpath.asPath,
                            "--patch-module", "$moduleName=${compileKotlinJvm.destinationDir}",
                            "-Xlint:-requires-transitive-automatic"
                        )
                    }
                }
                jvmJar.apply {
                    dependsOn(jvmCompileModuleInfo)
                    from(classesDir) {
                        into("META-INF/versions/9")
                    }
                    manifest {
                        attributes(jvmJar.manifest.attributes + mapOf("Multi-Release" to true))
                    }
                }
            }
        }
    }
}
