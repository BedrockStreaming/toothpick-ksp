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
package toothpick.compiler.factory

import com.google.common.truth.Truth
import com.google.testing.compile.JavaFileObjects
import com.google.testing.compile.JavaSourceSubjectFactory
import org.junit.Test
import toothpick.compiler.factory.ProcessorTestUtilities.factoryProcessorsFailingOnNonInjectableClasses

class RelaxedFactoryWarningsTest : BaseFactoryTest() {

    @Test
    fun testOptimisticFactoryCreationForSingleton_shouldFailTheBuild_whenThereIsNoDefaultConstructor() {
        val source = JavaFileObjects.forSourceString(
            "test.TestOptimisticFactoryCreationForSingleton",
            // language=java
            """
            package test;
            import javax.inject.Inject;
            import javax.inject.Singleton;
            @Singleton
            public class TestOptimisticFactoryCreationForSingleton {
              TestOptimisticFactoryCreationForSingleton(int a) { }
            }
            """.trimIndent()
        )

        Truth.assert_()
            .about(JavaSourceSubjectFactory.javaSource())
            .that(source)
            .processedWith(factoryProcessorsFailingOnNonInjectableClasses())
            .failsToCompile()
    }

    @Test
    fun testOptimisticFactoryCreationForSingleton_shouldNotFailTheBuild_whenThereIsNoDefaultConstructorButClassIsAnnotated() {
        val source = JavaFileObjects.forSourceString(
            "test.TestOptimisticFactoryCreationForSingleton",
            // language=java
            """
            package test;
            import javax.inject.Inject;
            import javax.inject.Singleton;
            @SuppressWarnings("injectable")
            @Singleton
            public class TestOptimisticFactoryCreationForSingleton {
              TestOptimisticFactoryCreationForSingleton(int a) { }
            }
            """.trimIndent()
        )

        Truth.assert_()
            .about(JavaSourceSubjectFactory.javaSource())
            .that(source)
            .processedWith(factoryProcessorsFailingOnNonInjectableClasses())
            .compilesWithoutError()
    }

    @Test
    fun testOptimisticFactoryCreationWithInjectedMembers_shouldFailTheBuild_whenThereIsNoDefaultConstructor() {
        val source = JavaFileObjects.forSourceString(
            "test.TestOptimisticFactoryCreationForSingleton",
            // language=java
            """
            package test;
            import javax.inject.Inject;
            public class TestOptimisticFactoryCreationForSingleton {
              @Inject String s;
              TestOptimisticFactoryCreationForSingleton(int a) { }
            }
            """.trimIndent()
        )

        Truth.assert_()
            .about(JavaSourceSubjectFactory.javaSource())
            .that(source)
            .processedWith(factoryProcessorsFailingOnNonInjectableClasses())
            .failsToCompile()
    }

    @Test
    fun testOptimisticFactoryCreationWithInjectedMembers_shouldNotFailTheBuild_whenThereIsNoDefaultConstructorButClassIsAnnotated() {
        val source = JavaFileObjects.forSourceString(
            "test.TestOptimisticFactoryCreationForSingleton",
            // language=java
            """
            package test;
            import javax.inject.Inject;
            @SuppressWarnings("injectable")
            public class TestOptimisticFactoryCreationForSingleton {
              @Inject String s;
              TestOptimisticFactoryCreationForSingleton(int a) { }
            }
            """.trimIndent()
        )

        Truth.assert_()
            .about(JavaSourceSubjectFactory.javaSource())
            .that(source)
            .processedWith(factoryProcessorsFailingOnNonInjectableClasses())
            .compilesWithoutError()
    }
}
