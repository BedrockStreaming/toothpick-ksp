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

import com.google.devtools.ksp.getClassDeclarationByName
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeParameter
import com.google.devtools.ksp.symbol.KSTypeReference
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

/**
 * Reified function to check if receiver [KSType] is assignable from [T] class
 */
inline fun <reified T> KSType.isAssignableFrom(resolver: Resolver): Boolean {
    val classDeclaration = requireNotNull(resolver.getClassDeclarationByName<T>()) {
        "Unable to resolve ${KSClassDeclaration::class.simpleName} for type ${T::class.simpleName}"
    }
    return isAssignableFrom(classDeclaration.asStarProjectedType())
}

val KSTypeReference.declaration get() = resolve().declaration as KSClassDeclaration

/**
 * @returns [KSAnnotation] for typed T or null if not found
 */
inline fun <reified T> KSDeclaration.findAnnotation(resolver: Resolver): KSAnnotation? {
    val annotationKsName = resolver.getKSNameFromString(T::class.simpleName!!)
    return annotations.firstOrNull { it.shortName.asString() == annotationKsName.asString() }
}

// KSTypeParameter

val KSType.singleTypeParameter get() = (declaration as KSTypeParameter).run { parentDeclaration!!.typeParameters.single { it.name == name } }

val KSDeclaration.parentClassDeclaration get() = parentDeclaration as KSClassDeclaration

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