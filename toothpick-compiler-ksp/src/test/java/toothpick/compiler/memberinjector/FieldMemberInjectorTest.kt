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

class FieldMemberInjectorTest {

    @Test
    fun testSimpleFieldInjection() {
        val source = JavaFileObjects.forSourceString(
            "test.TestFieldInjection",
            // language=java
            """
            package test;
            import javax.inject.Inject;
            public class TestFieldInjection {
              @Inject Foo foo;
              public TestFieldInjection() {}
            }
            class Foo {}
            """.trimIndent()
        )

        val expectedSource = JavaFileObjects.forSourceString(
            "test/TestFieldInjection__MemberInjector",
            // language=java
            """
            package test;
            
            import java.lang.Override;
            import toothpick.MemberInjector;
            import toothpick.Scope;
            
            public final class TestFieldInjection__MemberInjector implements MemberInjector<TestFieldInjection> {
              @Override
              public void inject(TestFieldInjection target, Scope scope) {
                target.foo = scope.getInstance(Foo.class);
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
    fun testNamedFieldInjection_whenUsingNamed() {
        val source = JavaFileObjects.forSourceString(
            "test.TestFieldInjection",
            // language=java
            """
            package test;
            import javax.inject.Inject;
            import javax.inject.Named;
            public class TestFieldInjection {
              @Inject @Named("bar") Foo foo;
              public TestFieldInjection() {}
            }
            class Foo {}
            """.trimIndent()
        )

        val expectedSource = JavaFileObjects.forSourceString(
            "test/TestFieldInjection__MemberInjector",
            // language=java
            """
            package test;
            
            import java.lang.Override;
            import toothpick.MemberInjector;
            import toothpick.Scope;
            
            public final class TestFieldInjection__MemberInjector implements MemberInjector<TestFieldInjection> {
              @Override
              public void inject(TestFieldInjection target, Scope scope) {
                target.foo = scope.getInstance(Foo.class, "bar");
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
    fun testNamedFieldInjection_whenUsingQualifierAnnotation() {
        val source = JavaFileObjects.forSourceString(
            "test.TestFieldInjection",
            // language=java
            """
            package test;
            import javax.inject.Inject;
            import javax.inject.Named;
            import javax.inject.Qualifier;
            public class TestFieldInjection {
              @Inject @Bar Foo foo;
              public TestFieldInjection() {}
            }
            class Foo {}
            @Qualifier
            @interface Bar {}
            """.trimIndent()
        )

        val expectedSource = JavaFileObjects.forSourceString(
            "test/TestFieldInjection__MemberInjector",
            // language=java
            """
            package test;
            
            import java.lang.Override;
            import toothpick.MemberInjector;
            import toothpick.Scope;
            
            public final class TestFieldInjection__MemberInjector implements MemberInjector<TestFieldInjection> {
              @Override
              public void inject(TestFieldInjection target, Scope scope) {
                target.foo = scope.getInstance(Foo.class, "test.Bar");
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
    fun testNamedFieldInjection_whenUsingNonQualifierAnnotation() {
        val source = JavaFileObjects.forSourceString(
            "test.TestFieldInjection",
            // language=java
            """
            package test;
            import javax.inject.Inject;
            import javax.inject.Named;
            public class TestFieldInjection {
              @Inject @Bar Foo foo;
              public TestFieldInjection() {}
            }
            class Foo {}
            @interface Bar {}
            """.trimIndent()
        )

        val expectedSource = JavaFileObjects.forSourceString(
            "test/TestFieldInjection__MemberInjector",
            // language=java
            """
            package test;
            
            import java.lang.Override;
            import toothpick.MemberInjector;
            import toothpick.Scope;
            
            public final class TestFieldInjection__MemberInjector implements MemberInjector<TestFieldInjection> {
              @Override
              public void inject(TestFieldInjection target, Scope scope) {
                target.foo = scope.getInstance(Foo.class);
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
    fun testNamedProviderFieldInjection_whenUsingQualifierAnnotation() {
        val source = JavaFileObjects.forSourceString(
            "test.TestFieldInjection",
            // language=java
            """
            package test;
            import javax.inject.Inject;
            import javax.inject.Named;
            import javax.inject.Provider;
            import javax.inject.Qualifier;
            public class TestFieldInjection {
              @Inject @Bar Provider<Foo> foo;
              public TestFieldInjection() {}
            }
            class Foo {}
            @Qualifier
            @interface Bar {}
            """.trimIndent()
        )

        val expectedSource = JavaFileObjects.forSourceString(
            "test/TestFieldInjection__MemberInjector",
            // language=java
            """
            package test;
            
            import java.lang.Override;
            import toothpick.MemberInjector;
            import toothpick.Scope;
            
            public final class TestFieldInjection__MemberInjector implements MemberInjector<TestFieldInjection> {
              @Override
              public void inject(TestFieldInjection target, Scope scope) {
                target.foo = scope.getProvider(Foo.class, "test.Bar");
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
    fun testNamedProviderFieldInjection_whenUsingNonQualifierAnnotation() {
        val source = JavaFileObjects.forSourceString(
            "test.TestFieldInjection",
            // language=java
            """
            package test;
            import javax.inject.Inject;
            import javax.inject.Named;
            import javax.inject.Provider;
            public class TestFieldInjection {
              @Inject @Bar Provider<Foo> foo;
              public TestFieldInjection() {}
            }
            class Foo {}
            @interface Bar {}
            """.trimIndent()
        )

        val expectedSource = JavaFileObjects.forSourceString(
            "test/TestFieldInjection__MemberInjector",
            // language=java
            """
            package test;
            
            import java.lang.Override;
            import toothpick.MemberInjector;
            import toothpick.Scope;
            
            public final class TestFieldInjection__MemberInjector implements MemberInjector<TestFieldInjection> {
              @Override
              public void inject(TestFieldInjection target, Scope scope) {
                target.foo = scope.getProvider(Foo.class);
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
    fun testNamedFieldInjection_shouldWork_whenUsingMoreThan2Annotation_butOnly1Qualifier() {
        val source = JavaFileObjects.forSourceString(
            "test.TestFieldInjection",
            // language=java
            """
            package test;
            import javax.inject.Inject;
            import javax.inject.Named;
            import javax.inject.Qualifier;
            public class TestFieldInjection {
              @Inject @Bar @Qurtz Foo foo;
              public TestFieldInjection() {}
            }
            class Foo {}
            @Qualifier
            @interface Bar {}
            @interface Qurtz {}
            """.trimIndent()
        )

        val expectedSource = JavaFileObjects.forSourceString(
            "test/TestFieldInjection__MemberInjector",
            // language=java
            """
            package test;
            
            import java.lang.Override;
            import toothpick.MemberInjector;
            import toothpick.Scope;
            
            public final class TestFieldInjection__MemberInjector implements MemberInjector<TestFieldInjection> {
              @Override
              public void inject(TestFieldInjection target, Scope scope) {
                target.foo = scope.getInstance(Foo.class, "test.Bar");
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
    fun testNamedFieldInjection_shouldFail_whenUsingMoreThan1QualifierAnnotations() {
        val source = JavaFileObjects.forSourceString(
            "test.TestFieldInjection",
            // language=java
            """
            package test;
            import javax.inject.Inject;
            import javax.inject.Named;
            import javax.inject.Qualifier;
            public class TestFieldInjection {
              @Inject @Bar @Qurtz Foo foo;
              public TestFieldInjection() {}
            }
            class Foo {}
            @Qualifier
            @interface Bar {}
            @Qualifier
            @interface Qurtz {}
            """.trimIndent()
        )

        Truth.assert_()
            .about(JavaSourceSubjectFactory.javaSource())
            .that(source)
            .processedWith(memberInjectorProcessors())
            .failsToCompile()
            .withErrorContaining(
                "Only one javax.inject.Qualifier annotation is allowed to name injections."
            )
    }

    @Test
    fun testProviderFieldInjection() {
        val source = JavaFileObjects.forSourceString(
            "test.TestFieldInjection",
            // language=java
            """
            package test;
            import javax.inject.Inject;
            import javax.inject.Provider;
            public class TestFieldInjection {
              @Inject Provider<Foo> foo;
              public TestFieldInjection() {}
            }
            class Foo {}
            """.trimIndent()
        )

        val expectedSource = JavaFileObjects.forSourceString(
            "test/TestFieldInjection__MemberInjector",
            // language=java
            """
            package test;
            
            import java.lang.Override;
            import toothpick.MemberInjector;
            import toothpick.Scope;
            
            public final class TestFieldInjection__MemberInjector implements MemberInjector<TestFieldInjection> {
              @Override
              public void inject(TestFieldInjection target, Scope scope) {
                target.foo = scope.getProvider(Foo.class);
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
    fun testLazyFieldInjection() {
        val source = JavaFileObjects.forSourceString(
            "test.TestFieldInjection",
            // language=java
            """
            package test;
            import javax.inject.Inject;
            import toothpick.Lazy;
            public class TestFieldInjection {
              @Inject Lazy<Foo> foo;
              public TestFieldInjection() {}
            }
            class Foo {}
            """.trimIndent()
        )

        val expectedSource = JavaFileObjects.forSourceString(
            "test/TestFieldInjection__MemberInjector",
            // language=java
            """
            package test;
            
            import java.lang.Override;
            import toothpick.MemberInjector;
            import toothpick.Scope;
            
            public final class TestFieldInjection__MemberInjector implements MemberInjector<TestFieldInjection> {
              @Override
              public void inject(TestFieldInjection target, Scope scope) {
                target.foo = scope.getLazy(Foo.class);
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
    fun testLazyFieldInjectionOfGenericTypeButNotDeclaringLazyOfGenericType() {
        val source = JavaFileObjects.forSourceString(
            "test.TestFieldInjection",
            // language=java
            """
            package test;
            import javax.inject.Inject;
            import toothpick.Lazy;
            public class TestFieldInjection {
              @Inject Lazy<Foo> foo;
              public TestFieldInjection() {}
            }
            class Foo<T> {}
            """.trimIndent()
        )

        val expectedSource = JavaFileObjects.forSourceString(
            "test/TestFieldInjection__MemberInjector",
            // language=java
            """
            package test;
            
            import java.lang.Override;
            import toothpick.MemberInjector;
            import toothpick.Scope;
            
            public final class TestFieldInjection__MemberInjector implements MemberInjector<TestFieldInjection> {
              @Override
              public void inject(TestFieldInjection target, Scope scope) {
                target.foo = scope.getLazy(Foo.class);
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
    fun testFieldInjection_shouldProduceMemberInjector_whenClassHas2Fields() {
        val source = JavaFileObjects.forSourceString(
            "test.TestFieldInjection",
            // language=java
            """
            package test;
            import javax.inject.Inject;
            public class TestFieldInjection {
              @Inject Foo foo;
              @Inject Foo foo2;
              public TestFieldInjection() {}
            }
            class Foo {}
            """.trimIndent()
        )

        val expectedSource = JavaFileObjects.forSourceString(
            "test/TestFieldInjection__MemberInjector",
            // language=java
            """
            package test;
            
            import java.lang.Override;
            import toothpick.MemberInjector;
            import toothpick.Scope;
            
            public final class TestFieldInjection__MemberInjector implements MemberInjector<TestFieldInjection> {
              @Override
              public void inject(TestFieldInjection target, Scope scope) {
                target.foo = scope.getInstance(Foo.class);
                target.foo2 = scope.getInstance(Foo.class);
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
    fun testFieldInjection_shouldFail_whenFieldIsPrivate() {
        val source = JavaFileObjects.forSourceString(
            "test.TestFieldInjection",
            // language=java
            """
            package test;
            import javax.inject.Inject;
            public class TestFieldInjection {
              @Inject private Foo foo;
              public TestFieldInjection() {}
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
                "@Inject annotated fields must be non private : test.TestFieldInjection#foo"
            )
    }

    @Test
    fun testFieldInjection_shouldFail_WhenContainingClassIsPrivate() {
        val source = JavaFileObjects.forSourceString(
            "test.TestFieldInjection",
            // language=java
            """
            package test;
            import javax.inject.Inject;
            public class TestFieldInjection {
              private static class InnerClass {
                @Inject Foo foo;
                public InnerClass() {}
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
    fun testFieldInjection_shouldFail_WhenFieldIsInvalidLazy() {
        val source = JavaFileObjects.forSourceString(
            "test.TestFieldInjection",
            // language=java
            """
            package test;
            import javax.inject.Inject;
            import toothpick.Lazy;
            public class TestFieldInjection {
              @Inject Lazy foo;
              public TestFieldInjection() {}
            }
            """.trimIndent()
        )

        Truth.assert_()
            .about(JavaSourceSubjectFactory.javaSource())
            .that(source)
            .processedWith(memberInjectorProcessors())
            .failsToCompile()
            .withErrorContaining("Field test.TestFieldInjection#foo is not a valid toothpick.Lazy.")
    }

    @Test
    fun testFieldInjection_shouldFail_WhenFieldIsInvalidProvider() {
        val source = JavaFileObjects.forSourceString(
            "test.TestFieldInjection",
            // language=java
            """
            package test;
            import javax.inject.Inject;
            import javax.inject.Provider;
            public class TestFieldInjection {
              @Inject Provider foo;
              public TestFieldInjection() {}
            }
            """.trimIndent()
        )

        Truth.assert_()
            .about(JavaSourceSubjectFactory.javaSource())
            .that(source)
            .processedWith(memberInjectorProcessors())
            .failsToCompile()
            .withErrorContaining(
                "Field test.TestFieldInjection#foo is not a valid javax.inject.Provider."
            )
    }

    @Test
    fun testFieldInjection_shouldFail_WhenFieldIsInvalidLazyGenerics() {
        val source = JavaFileObjects.forSourceString(
            "test.TestFieldInjection",
            // language=java
            """
            package test;
            import javax.inject.Inject;
            import toothpick.Lazy;
            public class TestFieldInjection {
              @Inject Lazy<Foo<String>> foo;
              public TestFieldInjection() {}
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
                "Lazy/Provider foo is not a valid in TestFieldInjection. Lazy/Provider cannot be used on generic types."
            )
    }

    @Test
    fun testFieldInjection_shouldFail_WhenFieldIsInvalidProviderGenerics() {
        val source = JavaFileObjects.forSourceString(
            "test.TestFieldInjection",
            // language=java
            """
            package test;
            import javax.inject.Inject;
            import javax.inject.Provider;
            public class TestFieldInjection {
              @Inject Provider<Foo<String>> foo;
              public TestFieldInjection() {}
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
                "Lazy/Provider foo is not a valid in TestFieldInjection. Lazy/Provider cannot be used on generic types."
            )
    }

    @Test
    fun testFieldInjection_shouldInjectAsAnInstanceOfSuperClass_whenSuperClassIsStaticHasInjectedMembers() {
        val source = JavaFileObjects.forSourceString(
            "test.TestMemberInjection",
            // language=java
            """
            package test;
            import javax.inject.Inject;
            class TestMemberInjection {
              public static class InnerSuperClass {
                @Inject Foo foo;
                public InnerSuperClass() {}
              }
              public static class InnerClass extends InnerSuperClass {
                @Inject Foo foo;
                public InnerClass() {}
              }
            }
            class Foo {}
            """.trimIndent()
        )

        val expectedSource = JavaFileObjects.forSourceString(
            "test/TestMemberInjection\$InnerClass__MemberInjector",
            // language=java
            """
            package test;
            
            import java.lang.Override;
            import toothpick.MemberInjector;
            import toothpick.Scope;
            
            public final class TestMemberInjection${'$'}InnerClass__MemberInjector implements MemberInjector<TestMemberInjection.InnerClass> {
              private MemberInjector<TestMemberInjection.InnerSuperClass> superMemberInjector = new test.TestMemberInjection${'$'}InnerSuperClass__MemberInjector();
              @Override
              public void inject(TestMemberInjection.InnerClass target, Scope scope) {
                superMemberInjector.inject(target, scope);
                target.foo = scope.getInstance(Foo.class);
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
    fun testMemberInjection_shouldInjectAsAnInstanceOfSuperClass_whenSuperClassHasInjectedMembers() {
        val source = JavaFileObjects.forSourceString(
            "test.TestMemberInjection",
            // language=java
            """
            package test;
            import javax.inject.Inject;
            public class TestMemberInjection extends TestMemberInjectionParent {
              @Inject Foo foo;
            }
            class TestMemberInjectionParent {
              @Inject Foo foo;
            }
            class Foo {}
            """.trimIndent()
        )

        val expectedSource = JavaFileObjects.forSourceString(
            "test/TestMemberInjection__MemberInjector",
            // language=java
            """
            package test;
            
            import java.lang.Override;
            import toothpick.MemberInjector;
            import toothpick.Scope;
            
            public final class TestMemberInjection__MemberInjector implements MemberInjector<TestMemberInjection> {
              private MemberInjector<TestMemberInjectionParent> superMemberInjector = new test.TestMemberInjectionParent__MemberInjector();
              @Override
              public void inject(TestMemberInjection target, Scope scope) {
                superMemberInjector.inject(target, scope);
                target.foo = scope.getInstance(Foo.class);
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
    fun testMemberInjection_shouldInjectAsAnInstanceOfSuperClass_whenSuperClassHasInjectedMembersAndTypeArgument() {
        val source = JavaFileObjects.forSourceString(
            "test.TestMemberInjection",
            // language=java
            """
            package test;
            import javax.inject.Inject;
            public class TestMemberInjection extends TestMemberInjectionParent<Integer> {
              @Inject Foo foo;
            }
            class TestMemberInjectionParent<T> {
              @Inject Foo foo;
            }
            class Foo {}
            """.trimIndent()
        )

        val expectedSource = JavaFileObjects.forSourceString(
            "test/TestMemberInjection__MemberInjector",
            // language=java
            """
            package test;
            
            import java.lang.Override;
            import toothpick.MemberInjector;
            import toothpick.Scope;
            
            public final class TestMemberInjection__MemberInjector implements MemberInjector<TestMemberInjection> {
              private MemberInjector<TestMemberInjectionParent> superMemberInjector = new test.TestMemberInjectionParent__MemberInjector();
              @Override
              public void inject(TestMemberInjection target, Scope scope) {
                superMemberInjector.inject(target, scope);
                target.foo = scope.getInstance(Foo.class);
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
    fun testFieldInjection_shouldFail_WhenFieldIsPrimitive() {
        val source = JavaFileObjects.forSourceString(
            "test.TestFieldInjection",
            // language=java
            """
            package test;
            import javax.inject.Inject;
            public class TestFieldInjection {
              @Inject int foo;
              public TestFieldInjection() {}
            }
            """.trimIndent()
        )

        Truth.assert_()
            .about(JavaSourceSubjectFactory.javaSource())
            .that(source)
            .processedWith(memberInjectorProcessors())
            .failsToCompile()
            .withErrorContaining(
                "Field test.TestFieldInjection#foo is of type int which is not supported by Toothpick."
            )
    }
}
