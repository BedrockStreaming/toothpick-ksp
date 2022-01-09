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

import com.google.common.truth.Truth
import com.google.testing.compile.JavaFileObjects
import com.google.testing.compile.JavaSourceSubjectFactory
import org.junit.Test
import toothpick.compiler.memberinjector.ProcessorTestUtilities.memberInjectorProcessors

class MethodMemberInjectorTest {

    @Test
    fun testSimpleMethodInjection() {
        val source = JavaFileObjects.forSourceString(
            "test.TestMethodInjection",
            // language=java
            """
            package test;
            import javax.inject.Inject;
            public class TestMethodInjection {
              @Inject
              public void m(Foo foo) {}
            }
            class Foo {}
            """.trimIndent()
        )

        val expectedSource = JavaFileObjects.forSourceString(
            "test/TestMethodInjection__MemberInjector",
            // language=java
            """
            package test;
            
            import java.lang.Override;
            import toothpick.MemberInjector;
            import toothpick.Scope;
            
            public final class TestMethodInjection__MemberInjector implements MemberInjector<TestMethodInjection> {
              @Override
              public void inject(TestMethodInjection target, Scope scope) {
                Foo param1 = scope.getInstance(Foo.class);
                target.m(param1);
              }
            }
            """.trimIndent()
        )
        Truth.assert_()
            .about(JavaSourceSubjectFactory.javaSource())
            .that(source)
            .processedWith(memberInjectorProcessors())
            .compilesWithoutError()
            .and()
            .generatesSources(expectedSource)
    }

    @Test
    fun testSimpleMethodInjectionWithLazy() {
        val source = JavaFileObjects.forSourceString(
            "test.TestMethodInjection",
            // language=java
            """
            package test;
            import javax.inject.Inject;
            import toothpick.Lazy;
            public class TestMethodInjection {
              @Inject
              public void m(Lazy<Foo> foo) {}
            }
            class Foo {}
            """.trimIndent()
        )

        val expectedSource = JavaFileObjects.forSourceString(
            "test/TestMethodInjection__MemberInjector",
            // language=java
            """
            package test;
            
            import java.lang.Override;
            import toothpick.Lazy;
            import toothpick.MemberInjector;
            import toothpick.Scope;
            
            public final class TestMethodInjection__MemberInjector implements MemberInjector<TestMethodInjection> {
              @Override
              public void inject(TestMethodInjection target, Scope scope) {
                Lazy<Foo> param1 = scope.getLazy(Foo.class);
                target.m(param1);
              }
            }
            """.trimIndent()
        )

        Truth.assert_()
            .about(JavaSourceSubjectFactory.javaSource())
            .that(source)
            .processedWith(memberInjectorProcessors())
            .compilesWithoutError()
            .and()
            .generatesSources(expectedSource)
    }

    @Test
    fun testSimpleMethodInjectionWithProvider() {
        val source = JavaFileObjects.forSourceString(
            "test.TestMethodInjection",
            // language=java
            """
            package test;
            import javax.inject.Inject;
            import javax.inject.Provider;
            public class TestMethodInjection {
              @Inject
              public void m(Provider<Foo> foo) {}
            }
            class Foo {}
            """.trimIndent()
        )

        val expectedSource = JavaFileObjects.forSourceString(
            "test/TestMethodInjection__MemberInjector",
            // language=java
            """
            package test;
            
            import java.lang.Override;
            import javax.inject.Provider;
            import toothpick.MemberInjector;
            import toothpick.Scope;
            
            public final class TestMethodInjection__MemberInjector implements MemberInjector<TestMethodInjection> {
              @Override
              public void inject(TestMethodInjection target, Scope scope) {
                Provider<Foo> param1 = scope.getProvider(Foo.class);
                target.m(param1);
              }
            }
            """.trimIndent()
        )

        Truth.assert_()
            .about(JavaSourceSubjectFactory.javaSource())
            .that(source)
            .processedWith(memberInjectorProcessors())
            .compilesWithoutError()
            .and()
            .generatesSources(expectedSource)
    }

    @Test
    fun testSimpleMethodInjectionWithLazyOfGenericTypeButNotLazyOfGenericType() {
        val source = JavaFileObjects.forSourceString(
            "test.TestMethodInjection",
            // language=java
            """
            package test;
            import javax.inject.Inject;
            import toothpick.Lazy;
            public class TestMethodInjection {
              @Inject
              public void m(Lazy<Foo> foo) {}
            }
            class Foo<T> {}
            """.trimIndent()
        )

        val expectedSource = JavaFileObjects.forSourceString(
            "test/TestMethodInjection__MemberInjector",
            // language=java
            """
            package test;
            
            import java.lang.Override;
            import toothpick.Lazy;
            import toothpick.MemberInjector;
            import toothpick.Scope;
            
            public final class TestMethodInjection__MemberInjector implements MemberInjector<TestMethodInjection> {
              @Override
              public void inject(TestMethodInjection target, Scope scope) {
                Lazy<Foo> param1 = scope.getLazy(Foo.class);
                target.m(param1);
              }
            }
            """.trimIndent()
        )

        Truth.assert_()
            .about(JavaSourceSubjectFactory.javaSource())
            .that(source)
            .processedWith(memberInjectorProcessors())
            .compilesWithoutError()
            .and()
            .generatesSources(expectedSource)
    }

    @Test
    fun testSimpleMethodInjectionWithLazyOfGenericType_shouldFail_WithLazyOfGenericType() {
        val source = JavaFileObjects.forSourceString(
            "test.TestMethodInjection",
            // language=java
            """
            package test;
            import javax.inject.Inject;
            import toothpick.Lazy;
            public class TestMethodInjection {
              @Inject
              public void m(Lazy<Foo<String>> foo) {}
            }
            class Foo<T> {}
            """.trimIndent()
        )

        Truth.assert_()
            .about(JavaSourceSubjectFactory.javaSource())
            .that(source)
            .processedWith(memberInjectorProcessors())
            .failsToCompile()
            .withErrorContaining(
                "Lazy/Provider foo is not a valid in m. Lazy/Provider cannot be used on generic types."
            )
    }

    @Test
    fun testMethodInjection_shouldFail_whenInjectedMethodIsPrivate() {
        val source = JavaFileObjects.forSourceString(
            "test.TestMethodInjection",
            // language=java
            """
            package test;
            import javax.inject.Inject;
            public class TestMethodInjection {
              @Inject
              private void m(Foo foo) {}
            }
            class Foo {}
            """.trimIndent()
        )

        Truth.assert_()
            .about(JavaSourceSubjectFactory.javaSource())
            .that(source)
            .processedWith(memberInjectorProcessors())
            .failsToCompile()
            .withErrorContaining(
                "@Inject annotated methods must not be private : test.TestMethodInjection#m"
            )
    }

    @Test
    fun testMethodInjection_shouldFail_whenContainingClassIsPrivate() {
        val source = JavaFileObjects.forSourceString(
            "test.TestMethodInjection",
            // language=java
            """
            package test;
            import javax.inject.Inject;
            public class TestMethodInjection {
              private static class InnerClass {
                @Inject
                public void m(Foo foo) {}
              }
            }
            class Foo {}
            """.trimIndent()
        )

        Truth.assert_()
            .about(JavaSourceSubjectFactory.javaSource())
            .that(source)
            .processedWith(memberInjectorProcessors())
            .failsToCompile()
            .withErrorContaining(
                "@Injected fields in class InnerClass. The class must be non private."
            )
    }

    @Test
    fun testMethodInjection_shouldFail_whenInjectedMethodParameterIsInvalidLazy() {
        val source = JavaFileObjects.forSourceString(
            "test.TestMethodInjection",
            // language=java
            """
            package test;
            import javax.inject.Inject;
            import toothpick.Lazy;
            public class TestMethodInjection {
              @Inject
              public void m(Lazy foo) {}
            }
            class Foo {}
            """.trimIndent()
        )

        Truth.assert_()
            .about(JavaSourceSubjectFactory.javaSource())
            .that(source)
            .processedWith(memberInjectorProcessors())
            .failsToCompile()
            .withErrorContaining(
                "Parameter foo in method/constructor test.TestMethodInjection#m is not a valid toothpick.Lazy."
            )
    }

    @Test
    fun testMethodInjection_shouldFail_whenInjectedMethodParameterIsInvalidProvider() {
        val source = JavaFileObjects.forSourceString(
            "test.TestMethodInjection",
            // language=java
            """
            package test;
            import javax.inject.Inject;
            import javax.inject.Provider;
            public class TestMethodInjection {
              @Inject
              public void m(Provider foo) {}
            }
            class Foo {}
            """.trimIndent()
        )

        Truth.assert_()
            .about(JavaSourceSubjectFactory.javaSource())
            .that(source)
            .processedWith(memberInjectorProcessors())
            .failsToCompile()
            .withErrorContaining(
                "Parameter foo in method/constructor test.TestMethodInjection#m is not a valid javax.inject.Provider."
            )
    }

    @Test
    fun testOverrideMethodInjection() {
        val source = JavaFileObjects.forSourceString(
            "test.TestMethodInjectionParent",
            // language=java
            """
            package test;
            import javax.inject.Inject;
            public class TestMethodInjectionParent {
              @Inject
              public void m(Foo foo) {}
              public static class TestMethodInjection extends TestMethodInjectionParent {
                @Inject
                public void m(Foo foo) {}
              }
            }
            class Foo {}
            """.trimIndent()
        )

        val expectedSource = JavaFileObjects.forSourceString(
            "test/TestMethodInjectionParent\$TestMethodInjection__MemberInjector",
            // language=java
            """
            package test;
            
            import java.lang.Override;
            import toothpick.MemberInjector;
            import toothpick.Scope;

            public final class TestMethodInjectionParent${'$'}TestMethodInjection__MemberInjector implements MemberInjector<TestMethodInjectionParent.TestMethodInjection> {
              private MemberInjector<TestMethodInjectionParent> superMemberInjector = new test.TestMethodInjectionParent__MemberInjector();\n
              @Override
              public void inject(TestMethodInjectionParent.TestMethodInjection target, Scope scope) {
                superMemberInjector.inject(target, scope);
              }
            }
            """.trimIndent()
        )

        Truth.assert_()
            .about(JavaSourceSubjectFactory.javaSource())
            .that(source)
            .processedWith(memberInjectorProcessors())
            .compilesWithoutError()
            .and()
            .generatesSources(expectedSource)
    }

    @Test
    fun testMethodInjection_withException() {
        val source = JavaFileObjects.forSourceString(
            "test.TestMethodInjection",
            // language=java
            """
            package test;
            import javax.inject.Inject;
            public class TestMethodInjection {
              @Inject
              public void m(Foo foo) throws Exception {}
            }
            class Foo {}
            """.trimIndent()
        )

        val expectedSource = JavaFileObjects.forSourceString(
            "test/TestMethodInjection__MemberInjector",
            // language=java
            """
            package test;
            
            import java.lang.Exception;
            import java.lang.Override;
            import java.lang.RuntimeException;
            import toothpick.MemberInjector;
            import toothpick.Scope;
            
            public final class TestMethodInjection__MemberInjector implements MemberInjector<TestMethodInjection> {
              @Override
              public void inject(TestMethodInjection target, Scope scope) {
                Foo param1 = scope.getInstance(Foo.class);
                try {
                  target.m(param1);
                } catch(Exception e1) {
                  throw new RuntimeException(e1);
                } 
              }
            }
            """.trimIndent()
        )

        Truth.assert_()
            .about(JavaSourceSubjectFactory.javaSource())
            .that(source)
            .processedWith(memberInjectorProcessors())
            .compilesWithoutError()
            .and()
            .generatesSources(expectedSource)
    }

    @Test
    fun testMethodInjection_withExceptions() {
        val source = JavaFileObjects.forSourceString(
            "test.TestMethodInjection",
            // language=java
            """
            package test;
            import javax.inject.Inject;
            public class TestMethodInjection {
              @Inject
              public void m(Foo foo) throws Exception, Throwable {}
            }
            class Foo {}
            """.trimIndent()
        )

        val expectedSource = JavaFileObjects.forSourceString(
            "test/TestMethodInjection__MemberInjector",
            // language=java
            """
            package test;
            
            import java.lang.Exception;
            import java.lang.Override;
            import java.lang.RuntimeException;
            import java.lang.Throwable;
            import toothpick.MemberInjector;
            import toothpick.Scope;
            
            public final class TestMethodInjection__MemberInjector implements MemberInjector<TestMethodInjection> {
              @Override
              public void inject(TestMethodInjection target, Scope scope) {
                Foo param1 = scope.getInstance(Foo.class);
                try {
                  target.m(param1);
                } catch(Exception e1) {
                  throw new RuntimeException(e1);
                } catch(Throwable e2) {
                  throw new RuntimeException(e2);
                } 
              }
            }
            """.trimIndent()
        )

        Truth.assert_()
            .about(JavaSourceSubjectFactory.javaSource())
            .that(source)
            .processedWith(memberInjectorProcessors())
            .compilesWithoutError()
            .and()
            .generatesSources(expectedSource)
    }
}