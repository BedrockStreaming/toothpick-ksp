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
package toothpick.compiler.memberinjector

import org.jetbrains.annotations.TestOnly
import toothpick.compiler.common.ToothpickProcessor
import toothpick.compiler.common.ToothpickProcessorOptions
import toothpick.compiler.common.generators.fields
import toothpick.compiler.common.generators.methods
import toothpick.compiler.memberinjector.generators.MemberInjectorGenerator
import toothpick.compiler.memberinjector.targets.FieldInjectionTarget
import toothpick.compiler.memberinjector.targets.MethodInjectionTarget
import javax.annotation.processing.RoundEnvironment
import javax.annotation.processing.SupportedAnnotationTypes
import javax.annotation.processing.SupportedOptions
import javax.inject.Inject
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.TypeElement
import javax.lang.model.element.VariableElement

/**
 * This processor's role is to create [MemberInjector]. We create factories in different
 * situations :
 *
 *
 *  * When a class `Foo` has an [javax.inject.Singleton] annotated field : <br></br>
 * --> we create a MemberInjector to inject `Foo` instances.
 *  * When a class `Foo` has an [javax.inject.Singleton] method : <br></br>
 * --> we create a MemberInjector to inject `Foo` instances.
 *
 */
// http://stackoverflow.com/a/2067863/693752
@SupportedAnnotationTypes(ToothpickProcessor.INJECT_ANNOTATION_CLASS_NAME)
@SupportedOptions(
    ToothpickProcessor.PARAMETER_EXCLUDES,
    ToothpickProcessor.PARAMETER_CRASH_WHEN_INJECTED_METHOD_IS_NOT_PACKAGE
)
open class MemberInjectorProcessor : ToothpickProcessor() {

    private val allRoundsGeneratedToTypeElement = mutableMapOf<String, TypeElement>()

    override fun process(annotations: Set<TypeElement>, roundEnv: RoundEnvironment): Boolean {
        val typeElementToFieldInjectorTargetList = mutableMapOf<TypeElement, List<FieldInjectionTarget>>()
        val typeElementToMethodInjectorTargetList = mutableMapOf<TypeElement, List<MethodInjectionTarget>>()
        val typeElementToSuperTypeElementThatNeedsInjection = mutableMapOf<TypeElement, TypeElement?>()

        roundEnv.findAndParseTargets(
            typeElementToFieldInjectorTargetList,
            typeElementToMethodInjectorTargetList,
            typeElementToSuperTypeElementThatNeedsInjection
        )

        // Generate member scopes
        val elementWithInjectionSet: Set<TypeElement> =
            typeElementToFieldInjectorTargetList.keys +
                typeElementToMethodInjectorTargetList.keys

        elementWithInjectionSet.forEach { typeElement ->
            val memberInjectorGenerator = MemberInjectorGenerator(
                targetClass = typeElement,
                superClassThatNeedsInjection = typeElementToSuperTypeElementThatNeedsInjection[typeElement],
                fieldInjectionTargetList = typeElementToFieldInjectorTargetList[typeElement],
                methodInjectionTargetList = typeElementToMethodInjectorTargetList[typeElement],
                typeUtil = typeUtils
            )

            writeToFile(
                codeGenerator = memberInjectorGenerator,
                fileDescription = "MemberInjector for type %s".format(typeElement)
            )

            allRoundsGeneratedToTypeElement[memberInjectorGenerator.fqcn] = typeElement
        }
        return false
    }

    private fun RoundEnvironment.findAndParseTargets(
        typeElementToFieldInjectorTargetList: MutableMap<TypeElement, List<FieldInjectionTarget>>,
        typeElementToMethodInjectorTargetList: MutableMap<TypeElement, List<MethodInjectionTarget>>,
        typeElementToSuperTypeElementThatNeedsInjection: MutableMap<TypeElement, TypeElement?>
    ) {
        processInjectAnnotatedFields(
            typeElementToFieldInjectorTargetList,
            typeElementToSuperTypeElementThatNeedsInjection
        )

        processInjectAnnotatedMethods(
            typeElementToMethodInjectorTargetList,
            typeElementToSuperTypeElementThatNeedsInjection
        )
    }

    private fun RoundEnvironment.processInjectAnnotatedFields(
        typeElementToFieldInjectorTargetList: MutableMap<TypeElement, List<FieldInjectionTarget>>,
        typeElementToSuperTypeElementThatNeedsInjection: MutableMap<TypeElement, TypeElement?>
    ) {
        getElementsAnnotatedWith(Inject::class.java)
            .fields
            .filterNot { (it.enclosingElement as TypeElement).isExcludedByFilters() }
            .forEach { element ->
                element.processInjectAnnotatedField(
                    typeElementToFieldInjectorTargetList,
                    typeElementToSuperTypeElementThatNeedsInjection
                )
            }
    }

    private fun RoundEnvironment.processInjectAnnotatedMethods(
        typeElementToMethodInjectorTargetList: MutableMap<TypeElement, List<MethodInjectionTarget>>,
        typeElementToSuperTypeElementThatNeedsInjection: MutableMap<TypeElement, TypeElement?>
    ) {
        getElementsAnnotatedWith(Inject::class.java)
            .methods
            .filterNot { (it.enclosingElement as TypeElement).isExcludedByFilters() }
            .forEach { element ->
                element.processInjectAnnotatedMethod(
                    typeElementToMethodInjectorTargetList,
                    typeElementToSuperTypeElementThatNeedsInjection
                )
            }
    }

    private fun VariableElement.processInjectAnnotatedField(
        typeElementToMemberInjectorTargetList: MutableMap<TypeElement, List<FieldInjectionTarget>>,
        typeElementToSuperTypeElementThatNeedsInjection: MutableMap<TypeElement, TypeElement?>
    ) {
        val enclosingElement = enclosingElement as TypeElement

        // Verify common generated code restrictions.
        if (!isValidInjectAnnotatedFieldOrParameter()) return

        val fieldInjectionTargetList: List<FieldInjectionTarget> =
            typeElementToMemberInjectorTargetList.getOrPut(enclosingElement) { emptyList() }

        typeElementToSuperTypeElementThatNeedsInjection[enclosingElement] =
            enclosingElement.getMostDirectSuperClassWithInjectedMembers(onlyParents = true)

        typeElementToMemberInjectorTargetList[enclosingElement] =
            fieldInjectionTargetList + createFieldOrParamInjectionTarget()
    }

    private fun ExecutableElement.processInjectAnnotatedMethod(
        typeElementToMemberInjectorTargetList: MutableMap<TypeElement, List<MethodInjectionTarget>>,
        typeElementToSuperTypeElementThatNeedsInjection: MutableMap<TypeElement, TypeElement?>
    ) {
        val enclosingElement = enclosingElement as TypeElement

        // Verify common generated code restrictions.
        if (!isValidInjectAnnotatedMethod()) {
            return
        }

        val methodInjectionTargetList: List<MethodInjectionTarget> =
            typeElementToMemberInjectorTargetList.getOrPut(enclosingElement) { emptyList() }

        typeElementToSuperTypeElementThatNeedsInjection[enclosingElement] =
            enclosingElement.getMostDirectSuperClassWithInjectedMembers(onlyParents = true)

        typeElementToMemberInjectorTargetList[enclosingElement] =
            methodInjectionTargetList + createMethodInjectionTarget()
    }

    private fun ExecutableElement.createMethodInjectionTarget(): MethodInjectionTarget {
        val enclosingElement = enclosingElement as TypeElement
        return MethodInjectionTarget(
            enclosingClass = enclosingElement,
            methodName = simpleName.toString(),
            isOverride = enclosingElement.isOverride(this),
            parameters = getParamInjectionTargetList(),
            exceptionTypes = getExceptionTypes()
        )
    }

    @TestOnly
    internal fun setCrashOrWarnWhenMethodIsNotPackageVisible(crashOrWarnWhenMethodIsNotPackageVisible: Boolean) {
        val current = optionsOverride ?: ToothpickProcessorOptions()
        optionsOverride = current.copy(
            crashWhenInjectedMethodIsNotPackageVisible = crashOrWarnWhenMethodIsNotPackageVisible
        )
    }

    @TestOnly
    internal fun getOriginatingElement(generatedQualifiedName: String): TypeElement? {
        return allRoundsGeneratedToTypeElement[generatedQualifiedName]
    }
}