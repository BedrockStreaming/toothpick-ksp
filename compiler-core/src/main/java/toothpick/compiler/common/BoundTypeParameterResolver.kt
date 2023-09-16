/*
 * Copyright 2022 Baptiste Candellier
 * Copyright 2019 Stephane Nicolas
 * Copyright 2019 Daniel Molinero Reguera
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
