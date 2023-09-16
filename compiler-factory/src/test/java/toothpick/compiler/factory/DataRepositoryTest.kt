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

class DataRepositoryTest {

    @Test
    fun testDataRepository() {
        val source = ktSource(
            "TestDataRepository",
            """
            package test
            import javax.inject.Inject
            import kotlin.reflect.KClass
            interface WebSocketClient
            interface Dto<out T> { fun get(): T }
            interface TestRepository<Network : Dto<Domain>, Domain : Any>
            class TestDataRepository<Network : Dto<Domain>, Domain : Any> @Inject constructor(
                private val webSocketClient: WebSocketClient,
                private val tClass: KClass<Network>, 
            ) : TestRepository<Network, Domain>
            """
        )

        val expected = expectedKtSource(
            "test/TestDataRepository__Factory",
            """
            package test

            import kotlin.Boolean
            import kotlin.Suppress
            import kotlin.reflect.KClass
            import toothpick.Factory
            import toothpick.Scope

            @Suppress(
              "ClassName",
              "RedundantVisibilityModifier",
            )
            public class TestDataRepository__Factory : Factory<TestDataRepository<*, *>> {
              @Suppress(
                "UNCHECKED_CAST",
                "NAME_SHADOWING",
              )
              public override fun createInstance(scope: Scope): TestDataRepository<*, *> {
                val scope = getTargetScope(scope)
                val param1 = scope.getInstance(WebSocketClient::class.java) as WebSocketClient
                val param2 = scope.getInstance(KClass::class.java) as KClass<test.Dto<kotlin.Any>>
                return TestDataRepository(param1, param2)
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

        compilationAssert()
            .that(source)
            .processedWith(FactoryProcessorProvider())
            .compilesWithoutError()
            .generatesSources(expected)
    }
}
