/*
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
package toothpick.compiler.memberinjector.generators

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import toothpick.MemberInjector
import toothpick.Scope
import toothpick.compiler.common.generators.CodeGenerator
import toothpick.compiler.common.generators.generatedFQNClassName
import toothpick.compiler.common.generators.generatedSimpleClassName
import toothpick.compiler.memberinjector.targets.FieldInjectionTarget
import toothpick.compiler.memberinjector.targets.MethodInjectionTarget
import javax.lang.model.element.TypeElement
import javax.lang.model.util.Types

/**
 * Generates a [MemberInjector] for a given collection of [FieldInjectionTarget].
 * Typically a [MemberInjector] is created for a class a soon as it contains an [ ] annotated field or method.
 */
class MemberInjectorGenerator(
    private val targetClass: TypeElement,
    private val superClassThatNeedsInjection: TypeElement?,
    private val fieldInjectionTargetList: List<FieldInjectionTarget>?,
    private val methodInjectionTargetList: List<MethodInjectionTarget>?,
    types: Types
) : CodeGenerator(types) {

    init {
        require(!(fieldInjectionTargetList == null && methodInjectionTargetList == null)) {
            "At least one memberInjectorInjectionTarget is needed."
        }
    }

    override fun brewJava(): String {
        // Interface to implement
        val className = targetClass.asClassName()

        // Build class
        return FileSpec.get(
            className.packageName,
            TypeSpec.classBuilder(targetClass.generatedSimpleClassName + MEMBER_INJECTOR_SUFFIX)
                .addModifiers(KModifier.PUBLIC, KModifier.FINAL)
                .addSuperinterface(
                    MemberInjector::class.asClassName().parameterizedBy(className)
                )
                .emitSuperMemberInjectorFieldIfNeeded()
                .emitInjectMethod(fieldInjectionTargetList, methodInjectionTargetList)
                .build()
        ).toString()
    }

    private fun TypeSpec.Builder.emitSuperMemberInjectorFieldIfNeeded(): TypeSpec.Builder = apply {
        if (superClassThatNeedsInjection == null) {
            return this
        }

        addProperty(
            // TODO use proper typing here
            PropertySpec.builder(
                "superMemberInjector",
                MemberInjector::class.asClassName()
                    .parameterizedBy(
                        typeUtil.erasure(superClassThatNeedsInjection.asType()).asTypeName()
                    ),
                KModifier.PRIVATE
            )
                .initializer(
                    "\$L__MemberInjector()",
                    superClassThatNeedsInjection.generatedFQNClassName
                )
                .build()
        )
    }

    private fun TypeSpec.Builder.emitInjectMethod(
        fieldInjectionTargetList: List<FieldInjectionTarget>?,
        methodInjectionTargetList: List<MethodInjectionTarget>?
    ): TypeSpec.Builder = apply {
        addFunction(
            FunSpec.builder("inject")
                .addAnnotation(Override::class)
                .addModifiers(KModifier.PUBLIC)
                .addParameter("target", targetClass.asClassName())
                .addParameter("scope", Scope::class)
                .apply {
                    if (superClassThatNeedsInjection != null) {
                        addStatement("superMemberInjector.inject(target, scope)")
                    }
                }
                .emitInjectFields(fieldInjectionTargetList)
                .emitInjectMethods(methodInjectionTargetList)
                .build()
        )
    }

    private fun FunSpec.Builder.emitInjectMethods(
        methodInjectionTargetList: List<MethodInjectionTarget>?
    ): FunSpec.Builder = apply {
        methodInjectionTargetList
            ?.filterNot { it.isOverride }
            ?.forEach { methodInjectionTarget ->
                methodInjectionTarget.parameters
                    .forEachIndexed { paramIndex, paramInjectionTarget ->
                        addStatement(
                            "\$T \$L = scope.\$L",
                            paramInjectionTarget.getParamType(),
                            "param${paramIndex + 1}",
                            paramInjectionTarget.getInvokeScopeGetMethodWithNameCodeBlock()
                        )
                    }


                val isMethodThrowingExceptions = methodInjectionTarget.exceptionTypes.isNotEmpty()
                if (isMethodThrowingExceptions) {
                    beginControlFlow("try")
                }

                addStatement(
                    "target.\$L(\$L)",
                    methodInjectionTarget.methodName,
                    List(methodInjectionTarget.parameters.size) { paramIndex -> "param${paramIndex + 1}" }
                        .joinToString(", ")
                )

                if (isMethodThrowingExceptions) {
                    methodInjectionTarget.exceptionTypes.forEachIndexed { exceptionCounter, exceptionType ->
                        nextControlFlow("catch (e\$L: \$T)", exceptionType, exceptionCounter)
                        addStatement(
                            "throw \$T(e\$L)", RuntimeException::class, exceptionCounter
                        )
                    }

                    endControlFlow()
                }
            }
    }

    private fun FunSpec.Builder.emitInjectFields(
        fieldInjectionTargetList: List<FieldInjectionTarget>?
    ): FunSpec.Builder = apply {
        fieldInjectionTargetList?.forEach { memberInjectionTarget ->
            addStatement(
                "target.\$L = scope.\$L",
                memberInjectionTarget.memberName,
                memberInjectionTarget.getInvokeScopeGetMethodWithNameCodeBlock()
            )
        }
    }

    override val fqcn: String = targetClass.generatedFQNClassName + MEMBER_INJECTOR_SUFFIX

    companion object {
        private const val MEMBER_INJECTOR_SUFFIX = "__MemberInjector"
    }
}