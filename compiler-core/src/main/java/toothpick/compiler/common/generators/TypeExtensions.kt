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
import com.google.devtools.ksp.symbol.KSTypeParameter
import com.google.devtools.ksp.symbol.Variance
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.STAR
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeVariableName
import com.squareup.kotlinpoet.WildcardTypeName
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

val Variance.modifier
    get() = when (this) {
        Variance.COVARIANT -> KModifier.OUT
        Variance.CONTRAVARIANT -> KModifier.IN
        else -> null
    }

fun TypeName.toTypeVariableName(bounds: List<TypeName>, modifier: KModifier?) =
    TypeVariableName(buildShortName(), bounds, modifier)

fun TypeName.buildShortName(): String = when (this) {
    is ClassName -> canonicalName
    is TypeVariableName -> name
    is WildcardTypeName -> when {
        inTypes.size == 1 -> "in·${inTypes[0].buildShortName()}"
        outTypes == STAR.outTypes -> "*"
        else -> "out·${inTypes[0].buildShortName()}"
    }
    is ParameterizedTypeName ->
        rawType.canonicalName + typeArguments.joinToString(", ", "<", ">") { it.buildShortName() }
    else -> toString()
}

fun KSTypeParameter.toBoundTypeVariableName(typeParameterResolver: TypeParameterResolver): TypeVariableName {
    val bounds = bounds.map { it.toTypeName(typeParameterResolver) }
    return bounds.firstOrNull()?.toTypeVariableName(bounds.drop(1).toList(), variance.modifier)
        ?: TypeVariableName("*", STAR)
}
