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
import javax.lang.model.util.ElementFilter

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

    private var mapTypeElementToFieldInjectorTargetList =
        mutableMapOf<TypeElement, MutableList<FieldInjectionTarget>>()

    private var mapTypeElementToMethodInjectorTargetList =
        mutableMapOf<TypeElement, MutableList<MethodInjectionTarget>>()

    private var mapTypeElementToSuperTypeElementThatNeedsInjection =
        mutableMapOf<TypeElement, TypeElement?>()

    private val allRoundsGeneratedToTypeElement = mutableMapOf<String, TypeElement>()

    override fun process(annotations: Set<TypeElement>, roundEnv: RoundEnvironment): Boolean {
        mapTypeElementToFieldInjectorTargetList = mutableMapOf()
        mapTypeElementToMethodInjectorTargetList = mutableMapOf()
        mapTypeElementToSuperTypeElementThatNeedsInjection = mutableMapOf()

        findAndParseTargets(roundEnv)

        // Generate member scopes
        val elementWithInjectionSet: MutableSet<TypeElement> = HashSet()
        elementWithInjectionSet.addAll(mapTypeElementToFieldInjectorTargetList.keys)
        elementWithInjectionSet.addAll(mapTypeElementToMethodInjectorTargetList.keys)

        for (typeElement in elementWithInjectionSet) {
            val fieldInjectionTargetList: List<FieldInjectionTarget>? =
                mapTypeElementToFieldInjectorTargetList[typeElement]

            val methodInjectionTargetList: List<MethodInjectionTarget>? =
                mapTypeElementToMethodInjectorTargetList[typeElement]

            val superClassThatNeedsInjection: TypeElement? =
                mapTypeElementToSuperTypeElementThatNeedsInjection[typeElement]

            val memberInjectorGenerator = MemberInjectorGenerator(
                typeElement,
                superClassThatNeedsInjection,
                fieldInjectionTargetList,
                methodInjectionTargetList,
                typeUtils
            )

            writeToFile(
                codeGenerator = memberInjectorGenerator,
                fileDescription = "MemberInjector for type %s".format(typeElement)
            )

            allRoundsGeneratedToTypeElement[memberInjectorGenerator.fqcn] = typeElement
        }
        return false
    }

    private fun findAndParseTargets(roundEnv: RoundEnvironment) {
        processInjectAnnotatedFields(roundEnv)
        processInjectAnnotatedMethods(roundEnv)
    }

    private fun processInjectAnnotatedFields(roundEnv: RoundEnvironment) {
        ElementFilter.fieldsIn(roundEnv.getElementsAnnotatedWith(Inject::class.java))
            .filterNot { (it.enclosingElement as TypeElement).isExcludedByFilters() }
            .forEach { it.processInjectAnnotatedField(mapTypeElementToFieldInjectorTargetList) }
    }

    private fun processInjectAnnotatedMethods(roundEnv: RoundEnvironment) {
        ElementFilter.methodsIn(roundEnv.getElementsAnnotatedWith(Inject::class.java))
            .filterNot { (it.enclosingElement as TypeElement).isExcludedByFilters() }
            .forEach { it.processInjectAnnotatedMethod(mapTypeElementToMethodInjectorTargetList) }
    }

    private fun VariableElement.processInjectAnnotatedField(
        mapTypeElementToMemberInjectorTargetList: MutableMap<TypeElement, MutableList<FieldInjectionTarget>>
    ) {
        val enclosingElement = enclosingElement as TypeElement

        // Verify common generated code restrictions.
        if (!isValidInjectAnnotatedFieldOrParameter()) return

        var fieldInjectionTargetList = mapTypeElementToMemberInjectorTargetList[enclosingElement]
        if (fieldInjectionTargetList == null) {
            fieldInjectionTargetList = ArrayList()
            mapTypeElementToMemberInjectorTargetList[enclosingElement] = fieldInjectionTargetList
        }

        enclosingElement.mapTypeToMostDirectSuperTypeThatNeedsInjection()
        fieldInjectionTargetList.add(
            createFieldOrParamInjectionTarget()
        )
    }

    private fun ExecutableElement.processInjectAnnotatedMethod(
        mapTypeElementToMemberInjectorTargetList: MutableMap<TypeElement, MutableList<MethodInjectionTarget>>?
    ) {
        val enclosingElement = enclosingElement as TypeElement

        // Verify common generated code restrictions.
        if (!isValidInjectAnnotatedMethod()) {
            return
        }
        var methodInjectionTargetList = mapTypeElementToMemberInjectorTargetList!![enclosingElement]
        if (methodInjectionTargetList == null) {
            methodInjectionTargetList = ArrayList()
            mapTypeElementToMemberInjectorTargetList[enclosingElement] = methodInjectionTargetList
        }

        enclosingElement.mapTypeToMostDirectSuperTypeThatNeedsInjection()
        methodInjectionTargetList.add(
            createMethodInjectionTarget()
        )
    }

    private fun TypeElement.mapTypeToMostDirectSuperTypeThatNeedsInjection() {
        val superClassWithInjectedMembers = getMostDirectSuperClassWithInjectedMembers(onlyParents = true)
        mapTypeElementToSuperTypeElementThatNeedsInjection[this] = superClassWithInjectedMembers
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