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
package toothpick.compiler.factory.generators

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import toothpick.Factory
import toothpick.MemberInjector
import toothpick.Scope
import toothpick.compiler.common.generators.CodeGenerator
import toothpick.compiler.factory.targets.ConstructorInjectionTarget
import javax.inject.Singleton
import javax.lang.model.util.Types

/**
 * Generates a [Factory] for a given [ConstructorInjectionTarget]. Typically a factory
 * is created for a class a soon as it contains an [javax.inject.Inject] annotated
 * constructor. See Optimistic creation of factories in TP wiki.
 */
class FactoryGenerator(
    private val constructorInjectionTarget: ConstructorInjectionTarget,
    types: Types
) : CodeGenerator(types) {

    override fun brewJava(): String {
        // Interface to implement
        val className = constructorInjectionTarget.builtClass.asClassName()
        val parameterizedTypeName = Factory::class.asClassName().parameterizedBy(className)
        val factoryClassName = constructorInjectionTarget.builtClass.generatedFQNClassName + FACTORY_SUFFIX

        // Build class
        val factoryTypeSpec = TypeSpec.classBuilder(factoryClassName)
            .addModifiers(KModifier.PUBLIC, KModifier.FINAL)
            .addSuperinterface(parameterizedTypeName)
            .emitSuperMemberInjectorFieldIfNeeded()
            .emitCreateInstance()
            .emitGetTargetScope()
            .emitHasScopeAnnotation()
            .emitHasSingletonAnnotation()
            .emitHasReleasableAnnotation()
            .emitHasProvidesSingletonAnnotation()
            .emitHasProvidesReleasableAnnotation()
            .build()

        return FileSpec.get(className.packageName, factoryTypeSpec).toString()
    }

    private fun TypeSpec.Builder.emitSuperMemberInjectorFieldIfNeeded() = apply {
        val superTypeThatNeedsInjection =
            constructorInjectionTarget.superClassThatNeedsMemberInjection?.asClassName()
                ?: return this

        val memberInjectorSuper: ParameterizedTypeName =
            MemberInjector::class.asClassName()
                .parameterizedBy(superTypeThatNeedsInjection)

        addProperty(
            PropertySpec
                .builder("memberInjector", memberInjectorSuper, KModifier.PRIVATE)
                .initializer(
                    "\$L__MemberInjector()",
                        constructorInjectionTarget
                            .superClassThatNeedsMemberInjection
                            .generatedFQNClassName
                )
                .build()
        )
    }

    override val fqcn: String
        get() = constructorInjectionTarget.builtClass.generatedFQNClassName + FACTORY_SUFFIX

    private fun TypeSpec.Builder.emitCreateInstance(): TypeSpec.Builder = apply {
        val className = constructorInjectionTarget.builtClass.asClassName()
        val createInstanceBuilder =
            FunSpec.builder("createInstance")
                .addAnnotation(Override::class)
                .addModifiers(KModifier.PUBLIC)
                .addParameter("scope", Scope::class)
                .returns(className)
                .apply {
                    // change the scope to target scope so that all dependencies are created in the target scope
                    // and the potential injection take place in the target scope too
                    if (constructorInjectionTarget.parameters.isNotEmpty()
                        || constructorInjectionTarget.superClassThatNeedsMemberInjection != null
                    ) {
                        // We only need it when the constructor contains parameters or dependencies
                        addStatement("scope = getTargetScope(scope)")
                    }
                }

        val simpleClassName = className.simpleClassName
        val varName: String =
            className.simpleName[0].lowercaseChar() +
                className.simpleName.substring(1)

        val throwsThrowable = constructorInjectionTarget.throwsThrowable

        val codeBlockBuilder = CodeBlock.builder()
            .apply {
                if (throwsThrowable) {
                    beginControlFlow("try")
                }

                constructorInjectionTarget.parameters.forEachIndexed { i, param ->
                    addStatement(
                        "val \$L: \$T = scope.\$L",
                        "param${i + 1}",
                        param.getParamType(),
                        param.getInvokeScopeGetMethodWithNameCodeBlock()
                    )
                }

                addStatement(
                    "val \$L: \$T = \$T(\$L)",
                    varName,
                    simpleClassName,
                    simpleClassName,
                    List(constructorInjectionTarget.parameters.size) { i -> "param${i + 1}" }
                        .joinToString(", ")
                )

                if (constructorInjectionTarget.superClassThatNeedsMemberInjection != null) {
                    addStatement("memberInjector.inject(\$L, scope)", varName)
                }

                addStatement("return \$L", varName)

                if (throwsThrowable) {
                    nextControlFlow("catch(ex: \$T)", Throwable::class.asClassName())
                    addStatement("throw \$T(ex)", RuntimeException::class.asClassName())
                    endControlFlow()
                }
            }

        createInstanceBuilder.addCode(codeBlockBuilder.build())

        addFunction(createInstanceBuilder.build())
    }

    private fun TypeSpec.Builder.emitGetTargetScope(): TypeSpec.Builder = apply {
        addFunction(
            FunSpec.builder("getTargetScope")
                .addAnnotation(Override::class)
                .addModifiers(KModifier.PUBLIC)
                .addParameter("scope", Scope::class)
                .returns(Scope::class)
                .addStatement(
                    "return scope\$L",
                    parentScopeCodeBlock.toString()
                )
                .build()
        )
    }

    private fun TypeSpec.Builder.emitHasScopeAnnotation(): TypeSpec.Builder = apply {
        val scopeName = constructorInjectionTarget.scopeName
        val hasScopeAnnotation = scopeName != null
        addFunction(
            FunSpec.builder("hasScopeAnnotation")
                .addAnnotation(Override::class)
                .addModifiers(KModifier.PUBLIC)
                .returns(Boolean::class)
                .addStatement("return \$L", hasScopeAnnotation)
                .build()
        )
    }

    private fun TypeSpec.Builder.emitHasSingletonAnnotation(): TypeSpec.Builder = apply {
        addFunction(
            FunSpec.builder("hasSingletonAnnotation")
                .addAnnotation(Override::class)
                .addModifiers(KModifier.PUBLIC)
                .returns(Boolean::class)
                .addStatement(
                    "return \$L",
                    constructorInjectionTarget.hasSingletonAnnotation
                )
                .build()
        )
    }

    private fun TypeSpec.Builder.emitHasReleasableAnnotation(): TypeSpec.Builder {
        addFunction(
            FunSpec.builder("hasReleasableAnnotation")
                .addAnnotation(Override::class)
                .addModifiers(KModifier.PUBLIC)
                .returns(Boolean::class)
                .addStatement(
                    "return \$L",
                    constructorInjectionTarget.hasReleasableAnnotation
                )
                .build()
        )
        return this
    }

    private fun TypeSpec.Builder.emitHasProvidesSingletonAnnotation(): TypeSpec.Builder = apply {
        addFunction(
            FunSpec.builder("hasProvidesSingletonAnnotation")
                .addAnnotation(Override::class)
                .addModifiers(KModifier.PUBLIC)
                .returns(Boolean::class)
                .addStatement(
                    "return \$L",
                    constructorInjectionTarget.hasProvidesSingletonInScopeAnnotation
                )
                .build()
        )
    }

    private fun TypeSpec.Builder.emitHasProvidesReleasableAnnotation(): TypeSpec.Builder = apply {
        addFunction(
            FunSpec.builder("hasProvidesReleasableAnnotation")
                .addAnnotation(Override::class)
                .addModifiers(KModifier.PUBLIC)
                .returns(Boolean::class)
                .addStatement(
                    "return \$L",
                    constructorInjectionTarget.hasProvidesReleasableAnnotation
                )
                .build()
        )
    }

    private val parentScopeCodeBlock: CodeBlock
        get() = when (val scopeName = constructorInjectionTarget.scopeName) {
            null -> CodeBlock.of("")
            // there is no scope name or the current @Scoped annotation.
            Singleton::class.java.name -> CodeBlock.of(".getRootScope()")
            else -> CodeBlock.of(".getParentScope(\$L.class)", scopeName)
        }

    companion object {
        private const val FACTORY_SUFFIX = "__Factory"
    }
}