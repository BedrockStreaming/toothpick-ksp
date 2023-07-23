package toothpick.compiler.common

import com.google.devtools.ksp.symbol.KSTypeParameter
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.TypeVariableName
import com.squareup.kotlinpoet.ksp.TypeParameterResolver
import toothpick.compiler.common.generators.toBoundTypeVariableName

/**
 * Recursively resolves typeParameters to it`s first bound type.
 *
 * The returned [TypeVariableName.name] uses [ClassName.simpleName] and may produce an uncompilable code for cases, when any of parameter`s types is not imported.
 */
class BoundTypeParameterResolver(typeParameters: List<KSTypeParameter>) : TypeParameterResolver {
    private val mapByName = typeParameters.associateBy { it.name.getShortName() }
    override val parametersMap: MutableMap<String, TypeVariableName> = LinkedHashMap()
    override operator fun get(index: String): TypeVariableName = parametersMap.getOrPut(index) {
        buildVariableTypeName(index)
    }

    private fun buildVariableTypeName(index: String): TypeVariableName {
        val parameter = mapByName[index] ?: throw NoSuchElementException("No TypeParameter found for index $index")
        return parameter.toBoundTypeVariableName(this)
    }
}