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
package toothpick.compiler.factory

import org.junit.Test
import toothpick.compiler.compilationAssert
import toothpick.compiler.compilesWithoutError
import toothpick.compiler.expectedKtSource
import toothpick.compiler.generatesSources
import toothpick.compiler.ktSource
import toothpick.compiler.processedWith
import toothpick.compiler.that

@Suppress("PrivatePropertyName")
class ParametrizedParametersInjectionTest {

    @Test
    fun testParametrizedParametersInjection() {
        compilationAssert()
            .that(testParametrizedParametersInjection_source)
            .processedWith(FactoryProcessorProvider())
            .compilesWithoutError()
            .generatesSources(testParametrizedParametersInjection_expected)
    }

    private val testParametrizedParametersInjection_source = ktSource(
        "testParametrizedParametersInjection",
        """
            package test
            import javax.inject.Inject
            interface Nullable<out T> {
                fun get(): T
            }
            interface TestRepository<Nullable : Nullable<T>, T : Any>
            class TestParametrizedParametersInjection<T : Any> @Inject constructor(
                private val repository: TestRepository<Nullable<T>, T>,
            )
            """
    )

    private val testParametrizedParametersInjection_expected = expectedKtSource(
        "test/TestParametrizedParametersInjection__Factory",
        """
            package test

            import kotlin.Boolean
            import kotlin.Suppress
            import toothpick.Factory
            import toothpick.Scope

            @Suppress(
              "ClassName",
              "RedundantVisibilityModifier",
            )
            public class TestParametrizedParametersInjection__Factory :
                Factory<TestParametrizedParametersInjection<*>> {
              @Suppress(
                "UNCHECKED_CAST",
                "NAME_SHADOWING",
              )
              public override fun createInstance(scope: Scope): TestParametrizedParametersInjection<*> {
                val scope = getTargetScope(scope)
                val param1 = scope.getInstance(TestRepository::class.java) as
                    TestRepository<Nullable<kotlin.Any>, kotlin.Any>
                return TestParametrizedParametersInjection(param1)
              }

              public override fun getTargetScope(scope: Scope): Scope = scope

              public override fun hasScopeAnnotation(): Boolean = false

              public override fun hasSingletonAnnotation(): Boolean = false

              public override fun hasReleasableAnnotation(): Boolean = false

              public override fun hasProvidesSingletonAnnotation(): Boolean = false

              public override fun hasProvidesReleasableAnnotation(): Boolean = false
            }
            """
    )
}
