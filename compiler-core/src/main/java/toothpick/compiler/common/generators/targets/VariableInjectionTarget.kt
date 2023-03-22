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
@file:OptIn(KspExperimental::class)

package toothpick.compiler.common.generators.targets

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.isAnnotationPresent
import com.google.devtools.ksp.isLocal
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSName
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeAlias
import com.google.devtools.ksp.symbol.KSValueParameter
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.toTypeName
import toothpick.compiler.common.generators.error
import javax.inject.Named
import javax.inject.Qualifier

/**
 * Information necessary to identify the parameter of a method or a class's property.
 */
sealed class VariableInjectionTarget(
    val className: ClassName,
    val typeName: TypeName,
    val memberName: KSName,
    val qualifierName: Any?,
) {
    class Instance(
        className: ClassName,
        typeName: TypeName,
        memberName: KSName,
        qualifierName: Any?,
    ) : VariableInjectionTarget(className, typeName, memberName, qualifierName)

    class Lazy(
        className: ClassName,
        typeName: TypeName,
        memberName: KSName,
        qualifierName: Any?,
    ) : VariableInjectionTarget(className, typeName, memberName, qualifierName)

    class Provider(
        className: ClassName,
        typeName: TypeName,
        memberName: KSName,
        qualifierName: Any?,
    ) : VariableInjectionTarget(className, typeName, memberName, qualifierName)

    companion object {

        fun create(parameter: KSValueParameter, logger: KSPLogger? = null): VariableInjectionTarget =
            create(
                name = parameter.name!!,
                type = parameter.type.resolve(),
                qualifierName = parameter.findQualifierName(logger)
            )

        fun create(parameter: KSPropertyDeclaration, logger: KSPLogger? = null): VariableInjectionTarget =
            create(
                name = parameter.simpleName,
                type = parameter.type.resolve(),
                qualifierName = parameter.findQualifierName(logger)
            )

        private fun create(name: KSName, type: KSType, qualifierName: String?): VariableInjectionTarget =
            when (type.declaration.qualifiedName?.asString()) {
                javax.inject.Provider::class.qualifiedName -> {
                    val kindParamClass = type.getInjectedType()

                    Provider(
                        className = kindParamClass.toClassName(),
                        typeName = type.toParameterizedTypeName(kindParamClass),
                        memberName = name,
                        qualifierName = qualifierName
                    )
                }
                toothpick.Lazy::class.qualifiedName -> {
                    val kindParamClass = type.getInjectedType()

                    Lazy(
                        className = kindParamClass.toClassName(),
                        typeName = type.toParameterizedTypeName(kindParamClass),
                        memberName = name,
                        qualifierName = qualifierName
                    )
                }
                else -> createInstanceTarget(name, type, qualifierName)
            }

        private fun createInstanceTarget(name: KSName, type: KSType, qualifierName: String?): Instance {
            return if (type.declaration is KSTypeAlias) {
                val actualTypeClassName = type.findActualType().toClassName()
                val argumentsTypeNames = type.arguments.map { it.type!!.resolve().toTypeName() }

                Instance(
                    className = actualTypeClassName,
                    typeName = type.declaration.toClassName().parameterizedBy(argumentsTypeNames),
                    memberName = name,
                    qualifierName = qualifierName
                )
            } else {
                Instance(
                    className = type.toClassName(),
                    typeName = type.toTypeName(),
                    memberName = name,
                    qualifierName = qualifierName
                )
            }
        }

        private fun KSType.toParameterizedTypeName(kindParamClass: KSType): ParameterizedTypeName =
            toClassName().parameterizedBy(kindParamClass.toTypeName())

        /**
         * Lookup both [javax.inject.Qualifier] and [javax.inject.Named] to provide the name
         * of an injection.
         *
         * @receiver the node for which a qualifier is to be found.
         * @return the name of this injection, or null if it has no qualifier annotations.
         */
        private fun KSAnnotated.findQualifierName(logger: KSPLogger?): String? {
            val qualifierAnnotationNames = annotations
                .mapNotNull { annotation ->
                    val annotationClass = annotation.annotationType.resolve().declaration
                    val annotationClassName = annotationClass.qualifiedName?.asString()
                    if (annotationClass.isAnnotationPresent(Qualifier::class)) {
                        annotationClassName.takeIf { className ->
                            className != Named::class.qualifiedName
                        }
                    } else null
                }

            val namedValues = getAnnotationsByType(Named::class)
                .map { annotation -> annotation.value }

            val allNames = qualifierAnnotationNames + namedValues

            if (allNames.count() > 1) {
                logger?.error(this, "Only one javax.inject.Qualifier annotation is allowed to name injections.")
            }

            return allNames.firstOrNull()
        }

        /**
         * Retrieves the type of a field or param.
         *
         * @receiver The type to inspect.
         * @return Can be the type of a simple instance (e.g. in `b: B`, type is `B`).
         * But if the type is [toothpick.Lazy] or [javax.inject.Provider], then we use the type parameter
         * (e.g. in `Lazy<B>`, type is `B`, not `Lazy`).
         */
        private fun KSType.getInjectedType(): KSType = arguments.first().type!!.resolve()

        private fun KSType.findActualType(): KSType {
            val typeDeclaration = declaration
            return if (typeDeclaration is KSTypeAlias) {
                typeDeclaration.type.resolve().findActualType()
            } else {
                this
            }
        }

        /**
         * Copied from ksp [com.squareup.kotlinpoet.ksp.toClassNameInternal]
         * With it, we can create a correct type name for typealias (using [parameterizedBy])
         * Otherwise, we lose the generic parameters
         */
        private fun KSDeclaration.toClassName(): ClassName {
            require(!isLocal()) {
                "Local/anonymous classes are not supported!"
            }
            val pkgName = packageName.asString()
            val typesString = checkNotNull(qualifiedName).asString().removePrefix("$pkgName.")

            val simpleNames = typesString
                .split(".")
            return ClassName(pkgName, simpleNames)
        }
    }
}

fun VariableInjectionTarget.getInvokeScopeGetMethodWithNameCodeBlock(): CodeBlock {
    val scopeGetMethodName: String = when (this) {
        is VariableInjectionTarget.Instance -> "getInstance"
        is VariableInjectionTarget.Provider -> "getProvider"
        is VariableInjectionTarget.Lazy -> "getLazy"
    }

    return CodeBlock.builder()
        .add("%N(%T::class.java", scopeGetMethodName, className)
        .apply { if (qualifierName != null) add(", %S", qualifierName) }
        .add(")")
        .build()
}

fun VariableInjectionTarget.getParamType(): TypeName = typeName
