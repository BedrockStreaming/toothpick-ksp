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
import toothpick.compiler.factory.ProcessorTestUtilities.factoryAndMemberInjectorProcessors
import toothpick.compiler.factory.ProcessorTestUtilities.factoryProcessorsWithAdditionalTypes
import javax.tools.StandardLocation

class RelaxedFactoryForSingletonsTest : BaseFactoryTest() {

    @Test
    fun testOptimisticFactoryCreationForSingleton() {
        val source = JavaFileObjects.forSourceString(
            "test.TestOptimisticFactoryCreationForSingleton",
            // language=java
            """
            package test;
            import javax.inject.Inject;
            import javax.inject.Singleton;
            @Singleton
            public class TestOptimisticFactoryCreationForSingleton {
            }
            """.trimIndent()
        )
        Truth.assert_()
            .about(JavaSourceSubjectFactory.javaSource())
            .that(source)
            .processedWith(factoryAndMemberInjectorProcessors())
            .compilesWithoutError()
            .and()
            .generatesFileNamed(
                StandardLocation.locationFor("CLASS_OUTPUT"),
                "test",
                "TestOptimisticFactoryCreationForSingleton__Factory.class"
            )
    }

    @Test
    fun testOptimisticFactoryCreationForScopeAnnotation() {
        val source = JavaFileObjects.forSourceString(
            "test.TestOptimisticFactoryCreationForScopeAnnotation",
            // language=java
            """
            package test;
            import javax.inject.Inject;
            import javax.inject.Scope;
            import java.lang.annotation.Retention;
            import java.lang.annotation.RetentionPolicy;
            @Scope
            @Retention(RetentionPolicy.RUNTIME)
            @interface CustomScope {}
            @CustomScope
            public class TestOptimisticFactoryCreationForScopeAnnotation {
            }
            """.trimIndent()
        )
        Truth.assert_()
            .about(JavaSourceSubjectFactory.javaSource())
            .that(source)
            .processedWith(
                factoryProcessorsWithAdditionalTypes("test.CustomScope")
            )
            .compilesWithoutError()
            .and()
            .generatesFileNamed(
                StandardLocation.locationFor("CLASS_OUTPUT"),
                "test",
                "TestOptimisticFactoryCreationForScopeAnnotation__Factory.class"
            )
    }

    @Test
    fun testOptimisticFactoryCreationForScopeAnnotation_shouldFail_WhenScopeAnnotationDoesNotHaveRetention() {
        val source = JavaFileObjects.forSourceString(
            "test.TestOptimisticFactoryCreationForScopeAnnotation",
            // language=java
            """
            package test;
            import javax.inject.Inject;
            import javax.inject.Scope;
            import java.lang.annotation.Retention;
            import java.lang.annotation.RetentionPolicy;
            @Scope
            @interface CustomScope {}
            @CustomScope
            public class TestOptimisticFactoryCreationForScopeAnnotation {
            }
            """.trimIndent()
        )
        Truth.assert_()
            .about(JavaSourceSubjectFactory.javaSource())
            .that(source)
            .processedWith(
                factoryProcessorsWithAdditionalTypes("test.CustomScope")
            )
            .failsToCompile()
    }

    @Test
    fun testOptimisticFactoryCreationForScopeAnnotation_shouldFail_WhenScopeAnnotationDoesNotHaveRuntimeRetention() {
        val source = JavaFileObjects.forSourceString(
            "test.TestOptimisticFactoryCreationForScopeAnnotation",
            // language=java
            """
            package test;
            import javax.inject.Inject;
            import javax.inject.Scope;
            import java.lang.annotation.Retention;
            import java.lang.annotation.RetentionPolicy;
            @Scope
            @Retention(RetentionPolicy.CLASS)
            @interface CustomScope {}
            @CustomScope
            public class TestOptimisticFactoryCreationForScopeAnnotation {
            }
            """.trimIndent()
        )
        Truth.assert_()
            .about(JavaSourceSubjectFactory.javaSource())
            .that(source)
            .processedWith(
                factoryProcessorsWithAdditionalTypes("test.CustomScope")
            )
            .failsToCompile()
    }
}