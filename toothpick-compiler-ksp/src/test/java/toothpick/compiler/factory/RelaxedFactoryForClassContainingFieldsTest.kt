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

class RelaxedFactoryForClassContainingFieldsTest : BaseFactoryTest() {

    @Test
    fun testRelaxedFactoryCreationForInjectedField() {
        val source = JavaFileObjects.forSourceString(
            "test.TestRelaxedFactoryCreationForInjectField",
            // language=java
            """
            package test;
            import javax.inject.Inject;
            public class TestRelaxedFactoryCreationForInjectField {
              @Inject Foo foo;
            }
            class Foo {}
            """.trimIndent()
        )
        val expectedSource = JavaFileObjects.forSourceString(
            "test/TestRelaxedFactoryCreationForInjectField__Factory",
            // language=java
            """
            package test;
            import java.lang.Override;
            import toothpick.Factory;
            import toothpick.MemberInjector;
            import toothpick.Scope;
            
            public final class TestRelaxedFactoryCreationForInjectField__Factory implements Factory<TestRelaxedFactoryCreationForInjectField> {
              private MemberInjector<TestRelaxedFactoryCreationForInjectField> memberInjector = new test.TestRelaxedFactoryCreationForInjectField__MemberInjector();
              @Override
              public TestRelaxedFactoryCreationForInjectField createInstance(Scope scope) {
                scope = getTargetScope(scope);
                TestRelaxedFactoryCreationForInjectField testRelaxedFactoryCreationForInjectField = new TestRelaxedFactoryCreationForInjectField();
                memberInjector.inject(testRelaxedFactoryCreationForInjectField, scope);
                return testRelaxedFactoryCreationForInjectField;
              }
              @Override
              public Scope getTargetScope(Scope scope) {
                return scope;
              }
              @Override
              public boolean hasScopeAnnotation() {
                return false;
              }
              @Override
              public boolean hasSingletonAnnotation() {
                return false;
              }
              @Override
              public boolean hasReleasableAnnotation() {
                return false;
              }
              @Override
              public boolean hasProvidesSingletonAnnotation() {
                return false;
              }
              @Override
              public boolean hasProvidesReleasableAnnotation() {
                return false;
              }
            }
            """.trimIndent()
        )
        Truth.assert_()
            .about(JavaSourceSubjectFactory.javaSource())
            .that(source)
            .processedWith(factoryAndMemberInjectorProcessors())
            .compilesWithoutError()
            .and()
            .generatesSources(expectedSource)
    }

    @Test
    fun testRelaxedFactoryCreationForInjectedField_shouldFail_WhenFieldIsPrivate() {
        val source = JavaFileObjects.forSourceString(
            "test.TestRelaxedFactoryCreationForInjectField",
            // language=java
            """
            package test;
            import javax.inject.Inject;
            public class TestRelaxedFactoryCreationForInjectField {
              @Inject private Foo foo;
            }
            class Foo {}
            """.trimIndent()
        )
        Truth.assert_()
            .about(JavaSourceSubjectFactory.javaSource())
            .that(source)
            .processedWith(factoryAndMemberInjectorProcessors())
            .failsToCompile()
            .withErrorContaining(
                "@Inject annotated fields must be non private : test.TestRelaxedFactoryCreationForInjectField#foo"
            )
    }

    @Test
    fun testRelaxedFactoryCreationForInjectedField_shouldFail_WhenContainingClassIsPrivate() {
        val source = JavaFileObjects.forSourceString(
            "test.TestRelaxedFactoryCreationForInjectField",
            // language=java
            """
            package test;
            import javax.inject.Inject;
            public class TestRelaxedFactoryCreationForInjectField {
              private static class InnerClass {
                @Inject Foo foo;
              }
            }
            class Foo {}
            """.trimIndent()
        )
        Truth.assert_()
            .about(JavaSourceSubjectFactory.javaSource())
            .that(source)
            .processedWith(factoryAndMemberInjectorProcessors())
            .failsToCompile()
            .withErrorContaining(
                "@Injected fields in class InnerClass. The class must be non private."
            )
    }

    @Test
    fun testRelaxedFactoryCreationForInjectedField_shouldFail_WhenFieldIsInvalidLazy() {
        val source = JavaFileObjects.forSourceString(
            "test.TestRelaxedFactoryCreationForInjectField",
            // language=java
            """
            package test;
            import javax.inject.Inject;
            import toothpick.Lazy;
            public class TestRelaxedFactoryCreationForInjectField {
              @Inject Lazy foo;
            }
            class Foo {}
            """.trimIndent()
        )
        Truth.assert_()
            .about(JavaSourceSubjectFactory.javaSource())
            .that(source)
            .processedWith(factoryAndMemberInjectorProcessors())
            .failsToCompile()
            .withErrorContaining(
                "Field test.TestRelaxedFactoryCreationForInjectField#foo is not a valid toothpick.Lazy."
            )
    }

    @Test
    fun testRelaxedFactoryCreationForInjectedField_shouldFail_WhenFieldIsInvalidProvider() {
        val source = JavaFileObjects.forSourceString(
            "test.TestRelaxedFactoryCreationForInjectField",
            // language=java
            """
            package test;
            import javax.inject.Inject;
            import javax.inject.Provider;
            public class TestRelaxedFactoryCreationForInjectField {
              @Inject Provider foo;
            }
            class Foo {}
            """.trimIndent()
        )
        Truth.assert_()
            .about(JavaSourceSubjectFactory.javaSource())
            .that(source)
            .processedWith(factoryAndMemberInjectorProcessors())
            .failsToCompile()
            .withErrorContaining(
                "Field test.TestRelaxedFactoryCreationForInjectField#foo is not a valid javax.inject.Provider."
            )
    }

    @Test
    fun testRelaxedFactoryCreationForInjectedField_shouldWorkButNoFactoryIsProduced_whenTypeIsAbstract() {
        val source = JavaFileObjects.forSourceString(
            "test.TestRelaxedFactoryCreationForInjectField",
            // language=java
            """
            package test;
            import javax.inject.Inject;
            public abstract class TestRelaxedFactoryCreationForInjectField {
              @Inject Foo foo;
            }
            class Foo {}
            """.trimIndent()
        )
        assertThatCompileWithoutErrorButNoFactoryIsCreated(
            source, "test", "TestRelaxedFactoryCreationForInjectField"
        )
    }

    @Test
    fun testRelaxedFactoryCreationForInjectedField_shouldWorkButNoFactoryIsProduced_whenTypeHasANonDefaultConstructor() {
        val source = JavaFileObjects.forSourceString(
            "test.TestRelaxedFactoryCreationForInjectField",
            // language=java
            """
            package test;
            import javax.inject.Inject;
            public class TestRelaxedFactoryCreationForInjectField {
              @Inject Foo foo;
              public TestRelaxedFactoryCreationForInjectField(String s) {}
            }
            class Foo {}
            """.trimIndent()
        )
        assertThatCompileWithoutErrorButNoFactoryIsCreated(
            source, "test", "TestRelaxedFactoryCreationForInjectField"
        )
    }

    @Test
    fun testRelaxedFactoryCreationForInjectedField_shouldWorkButNoFactoryIsProduced_whenTypeHasAPrivateDefaultConstructor() {
        val source = JavaFileObjects.forSourceString(
            "test.TestRelaxedFactoryCreationForInjectField",
            // language=java
            """
            package test;
            import javax.inject.Inject;
            public class TestRelaxedFactoryCreationForInjectField {
              @Inject Foo foo;
              private TestRelaxedFactoryCreationForInjectField() {}
            }
            class Foo {}
            """.trimIndent()
        )
        assertThatCompileWithoutErrorButNoFactoryIsCreated(
            source, "test", "TestRelaxedFactoryCreationForInjectField"
        )
    }
}