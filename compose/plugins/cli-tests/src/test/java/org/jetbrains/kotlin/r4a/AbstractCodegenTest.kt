package org.jetbrains.kotlin.r4a

import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.config.addJvmClasspathRoots
import org.jetbrains.kotlin.codegen.GeneratedClassLoader
import org.jetbrains.kotlin.com.intellij.openapi.project.Project
import org.jetbrains.kotlin.com.intellij.openapi.util.text.StringUtilRt
import org.jetbrains.kotlin.com.intellij.openapi.vfs.CharsetToolkit
import org.jetbrains.kotlin.com.intellij.psi.PsiFileFactory
import org.jetbrains.kotlin.com.intellij.psi.impl.PsiFileFactoryImpl
import org.jetbrains.kotlin.com.intellij.testFramework.LightVirtualFile
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.AnalyzingUtils
import java.io.File

abstract class AbstractCodegenTest : AbstractCompilerTest() {
    override fun setUp() {
        super.setUp()
        val classPath = createClasspath() + additionalPaths

        val configuration = newConfiguration()
        configuration.addJvmClasspathRoots(classPath)
        updateConfiguration(configuration)

        additionalDependencies = listOf(
            assertExists(outputClassesJar("compose-runtime")),
            assertExists(outputClassesJar("ui-android-view-non-ir")),
            assertExists(
                File("../../../../../prebuilts/fullsdk-linux/platforms/android-28/android.jar")
            )
        )

        myEnvironment = KotlinCoreEnvironment.createForTests(
            myTestRootDisposable, configuration, EnvironmentConfigFiles.JVM_CONFIG_FILES
        ).also { installPlugins(it) }
    }

    open fun updateConfiguration(configuration: CompilerConfiguration) {
        configuration.put(JVMConfigurationKeys.IR, true)
        configuration.put(JVMConfigurationKeys.JVM_TARGET, JvmTarget.JVM_1_6)
    }

    open fun installPlugins(environment: KotlinCoreEnvironment) {
        R4AComponentRegistrar.registerProjectExtensions(
            environment.project,
            environment.configuration
        )
    }

    protected open fun helperFiles(): List<KtFile> = emptyList()

    protected fun dumpClasses(loader: GeneratedClassLoader) {
        for (file in loader.allGeneratedFiles.filter {
            it.relativePath.endsWith(".class")
        }) {
            println("------\nFILE: ${file.relativePath}\n------")
            println(file.asText())
        }
    }

    protected fun classLoader(
        source: String,
        fileName: String,
        dumpClasses: Boolean = false
    ): GeneratedClassLoader {
        val files = mutableListOf<KtFile>()
        files.addAll(helperFiles())
        files.add(sourceFile(fileName, source))
        myFiles = CodegenTestFiles.create(files)
        val loader = createClassLoader()
        if (dumpClasses) dumpClasses(loader)
        return loader
    }

    protected fun classLoader(
        sources: Map<String, String>,
        dumpClasses: Boolean = false
    ): GeneratedClassLoader {
        val files = mutableListOf<KtFile>()
        files.addAll(helperFiles())
        for ((fileName, source) in sources) {
            files.add(sourceFile(fileName, source))
        }
        myFiles = CodegenTestFiles.create(files)
        val loader = createClassLoader()
        if (dumpClasses) dumpClasses(loader)
        return loader
    }

    protected fun testFile(source: String, dumpClasses: Boolean = false) {
        val files = mutableListOf<KtFile>()
        files.addAll(helperFiles())
        files.add(sourceFile("Test.kt", source))
        myFiles = CodegenTestFiles.create(files)
        val loader = createClassLoader()
        if (dumpClasses) dumpClasses(loader)
        val loadedClass = loader.loadClass("Test")
        val instance = loadedClass.newInstance()
        val instanceClass = instance::class.java
        val testMethod = instanceClass.getMethod("test")
        testMethod.invoke(instance)
    }

    protected fun testCompile(source: String, dumpClasses: Boolean = false) {
        val files = mutableListOf<KtFile>()
        files.addAll(helperFiles())
        files.add(sourceFile("Test.kt", source))
        myFiles = CodegenTestFiles.create(files)
        val loader = createClassLoader()
        if (dumpClasses) dumpClasses(loader)
    }

    protected fun sourceFile(name: String, source: String): KtFile {
        val result = createFile(name, source, myEnvironment!!.project)
        val ranges = AnalyzingUtils.getSyntaxErrorRanges(result)
        assert(ranges.isEmpty()) { "Syntax errors found in $name: $ranges" }
        return result
    }

    protected fun loadClass(className: String, source: String): Class<*> {
        myFiles = CodegenTestFiles.create(
            "file.kt",
            source,
            myEnvironment!!.project
        )
        val loader = createClassLoader()
        return loader.loadClass(className)
    }

    protected open val additionalPaths = emptyList<File>()
}

fun createFile(name: String, text: String, project: Project): KtFile {
    var shortName = name.substring(name.lastIndexOf('/') + 1)
    shortName = shortName.substring(shortName.lastIndexOf('\\') + 1)
    val virtualFile = object : LightVirtualFile(
        shortName,
        KotlinLanguage.INSTANCE,
        StringUtilRt.convertLineSeparators(text)
    ) {
        override fun getPath(): String = "/$name"
    }

    virtualFile.setCharset(CharsetToolkit.UTF8_CHARSET)
    val factory = PsiFileFactory.getInstance(project) as PsiFileFactoryImpl

    return factory.trySetupPsiForFile(virtualFile, KotlinLanguage.INSTANCE, true, false) as KtFile
}
