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
import toothpick.compiler.common.ToothpickOptions.Companion.WrapScopeIntoWithControlFlow
import toothpick.compiler.compilationAssert
import toothpick.compiler.compilesWithoutError
import toothpick.compiler.expectedKtSource
import toothpick.compiler.generatesSources
import toothpick.compiler.ktSource
import toothpick.compiler.processedWith
import toothpick.compiler.that
import toothpick.compiler.withOptions

@Suppress("PrivatePropertyName")
class WrapScopeIntoWithControlFlowTest {

    @Test
    fun testNoWrapScopeIntoWithControlFlow() {
        compilationAssert()
            .that(testNoWrapScopeIntoWithControlFlow_source)
            .processedWith(FactoryProcessorProvider())
            .withOptions(WrapScopeIntoWithControlFlow to "false")
            .compilesWithoutError()
            .generatesSources(testNoWrapScopeIntoWithControlFlow_expected)
    }

    @Test
    fun testWrapScopeIntoWithControlFlow() {
        compilationAssert()
            .that(testWrapScopeIntoWithControlFlow_source)
            .processedWith(FactoryProcessorProvider())
            .withOptions(WrapScopeIntoWithControlFlow to "true")
            .compilesWithoutError()
            .generatesSources(testWrapScopeIntoWithControlFlow_expected)
    }

    private val testNoWrapScopeIntoWithControlFlow_source = ktSource(
        "testNoWrapScopeIntoWithControlFlow",
        """
            package test
            import javax.inject.Inject
            interface Nullable<out T> {
                fun get(): T
            }
            interface TestRepository<Nullable : Nullable<T>, T : Any>
            class TestNoWrapScopeIntoWithControlFlow<T : Any> @Inject constructor(
                private val repository: TestRepository<Nullable<T>, T>,
            )
            """
    )

    private val testNoWrapScopeIntoWithControlFlow_expected = expectedKtSource(
        "test/TestNoWrapScopeIntoWithControlFlow__Factory",
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
            public class TestNoWrapScopeIntoWithControlFlow__Factory :
                Factory<TestNoWrapScopeIntoWithControlFlow<*>> {
              @Suppress(
                "UNCHECKED_CAST",
                "NAME_SHADOWING",
              )
              public override fun createInstance(scope: Scope): TestNoWrapScopeIntoWithControlFlow<*> {
                val scope = getTargetScope(scope)
                val param1 = scope.getInstance(TestRepository::class.java) as
                    TestRepository<Nullable<kotlin.Any>, kotlin.Any>
                return TestNoWrapScopeIntoWithControlFlow(param1)
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

    private val testWrapScopeIntoWithControlFlow_source = ktSource(
        "TestConstructorWrap",
        """
            package test
            import javax.inject.Inject
            interface Nullable<out T> {
              fun get(): T
            }
            interface TestRepository<Nullable : Nullable<T>, T : Any>
            class TestWrapScopeIntoWithControlFlow<T: Any> @Inject constructor(
                private val repository: TestRepository<Nullable<T>, T>,
            )
            """
    )
    private val testWrapScopeIntoWithControlFlow_expected = expectedKtSource(
        "test/TestWrapScopeIntoWithControlFlow__Factory",
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
            public class TestWrapScopeIntoWithControlFlow__Factory :
                Factory<TestWrapScopeIntoWithControlFlow<*>> {
              @Suppress("UNCHECKED_CAST")
              public override fun createInstance(scope: Scope): TestWrapScopeIntoWithControlFlow<*> =
                  with(getTargetScope(scope)) {
                val param1 = getInstance(TestRepository::class.java) as
                    TestRepository<Nullable<kotlin.Any>, kotlin.Any>
                TestWrapScopeIntoWithControlFlow(param1)
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