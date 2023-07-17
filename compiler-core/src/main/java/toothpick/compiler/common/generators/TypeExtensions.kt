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
package toothpick.compiler.common.generators

import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSType
import com.squareup.kotlinpoet.STAR
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeVariableName
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.ksp.TypeParameterResolver
import com.squareup.kotlinpoet.ksp.toTypeName

/**
 * Alternative to [com.google.devtools.ksp.getAnnotationsByType] that retrieves [KSAnnotation]s instead.
 */
inline fun <reified T : Annotation> KSAnnotated.getAnnotationsByType(): Sequence<KSAnnotation> {
    return annotations.filter { annotation ->
        val className = T::class.asClassName()
        annotation.shortName.asString() == className.simpleName &&
            annotation.annotationType.resolve().declaration.qualifiedName?.asString() == className.canonicalName
    }
}

fun KSType.toErasedTypeName(): TypeName {
    return toTypeName(STAR_RESOLVER)
}

private val STAR_RESOLVER = object : TypeParameterResolver {
    override val parametersMap = emptyMap<String, TypeVariableName>()

    override fun get(index: String) = when (index) {
        "T" -> TypeVariableName("*", STAR)
        else -> throw NoSuchElementException("No TypeParameter found for index $index")
    }
}
