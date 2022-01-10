package toothpick.compiler

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import org.junit.Assert.assertEquals
import javax.annotation.processing.Processor

data class Builder(
    val sources: List<SourceFile> = emptyList(),
    val annotationProcessors: List<Processor> = emptyList()
)

fun compilationAssert(): Builder = Builder()

fun Builder.that(vararg sources: SourceFile): Builder =
    copy(sources = sources.toList())

fun Builder.processedWith(annotationProcessors: List<Processor>): Builder =
    copy(annotationProcessors = annotationProcessors)

private fun Builder.compile(configure: KotlinCompilation.() -> Unit = {}): KotlinCompilation.Result {
    val builder = this
    return KotlinCompilation().apply {
        inheritClassPath = true
        verbose = false
        this.sources = builder.sources
        this.annotationProcessors = builder.annotationProcessors
        configure()
    }.compile()
}

fun Builder.compilesWithoutError(): KotlinCompilation.Result = compile().apply {
    assertEquals(KotlinCompilation.ExitCode.OK, exitCode)
}

fun KotlinCompilation.Result.assertGeneratesSource(expected: RawSource) = apply {
    val actual = sourcesGeneratedByAnnotationProcessor.first { file -> file.name == expected.fileName }
    assertEquals(expected.contents, actual.readText())
}
