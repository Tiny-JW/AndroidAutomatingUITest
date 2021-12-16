import com.tiny.mock.BackendMockEndpointGenerator
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.io.File

@CacheableTask
abstract class BackendMockEndpointsGeneratorTask : DefaultTask() {

    @OutputDirectory
    val outputDir = File("${project.buildDir}/generated/source/backendMock")

    @TaskAction
    fun generateEndpoints() {
        val outputDirectoryWithPackage = File(outputDir, BackendMockEndpointGenerator.PACKAGE.replace('.', '/')).apply { mkdirs() }
        val outputFile = File(
            outputDirectoryWithPackage,
            BackendMockEndpointGenerator.FILENAME
        )
        if (outputFile.isRegenerationNeeded()) {
            outputFile.writeBytes(BackendMockEndpointGenerator.generate().toByteArray())
        }
        println("generateEndpoints: ${outputFile.absoluteFile}")
    }

    private fun File.isRegenerationNeeded() = !exists()
}