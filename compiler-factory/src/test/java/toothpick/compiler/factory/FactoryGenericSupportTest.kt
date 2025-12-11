package toothpick.compiler.factory

import org.junit.Test
import toothpick.compiler.common.generators.GenericSupportException
import toothpick.compiler.compilationAssert
import toothpick.compiler.compilesWithoutError
import toothpick.compiler.expectedKtSource
import toothpick.compiler.failsToCompile
import toothpick.compiler.generatesSources
import toothpick.compiler.ktSource
import toothpick.compiler.processedWith
import toothpick.compiler.that
import toothpick.compiler.withLogContaining

class FactoryGenericSupportTest {
    @Test
    fun typed_class_inheriting_generic_class() {
        val source = ktSource(
            "TestGeneric1",
            """
            package test
            import toothpick.InjectConstructor
            
            class GenericBaseClass<T> 
            
            @InjectConstructor
            class TestGeneric1 : GenericBaseClass<String>
            """
        )

        val expectedResult = expectedKtSource(
            "test/TestGeneric1__Factory",
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
            public class TestGeneric1__Factory : Factory<TestGeneric1> {
              public override fun createInstance(scope: Scope): TestGeneric1 = TestGeneric1()
            
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
            .generatesSources(expectedResult)
    }

    @Test
    fun generic_class_inheriting_generic_class() {
        val source = ktSource(
            "TestGeneric2",
            """
            package test
            import toothpick.InjectConstructor
            
            class GenericBaseClass<T> 
            
            @InjectConstructor
            class TestGeneric2<T> : GenericBaseClass<T>
            """
        )

        val expectedResult = expectedKtSource(
            "test/TestGeneric2__Factory",
            """
            package test
            
            import kotlin.Any
            import kotlin.Boolean
            import kotlin.Suppress
            import toothpick.Factory
            import toothpick.Scope

            @Suppress(
              "ClassName",
              "RedundantVisibilityModifier",
            )
            public class TestGeneric2__Factory : Factory<TestGeneric2<*>> {
              public override fun createInstance(scope: Scope): TestGeneric2<Any?> = TestGeneric2<Any?>()
            
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
            .generatesSources(expectedResult)
    }

    @Test
    fun generic_class_two_type_params_two_bounds() {
        val source = ktSource(
            "TestGeneric6",
            """
            package test
            import java.lang.Exception
            import toothpick.InjectConstructor
            
            
            @InjectConstructor
            class TestGeneric6<T, Q> where T : Exception, Q: Any
            """
        )

        val expectedResult =
            expectedKtSource(
                "test/TestGeneric6__Factory",
                """
            package test
            
            import java.lang.Exception
            import kotlin.Any
            import kotlin.Boolean
            import kotlin.Suppress
            import toothpick.Factory
            import toothpick.Scope

            @Suppress(
              "ClassName",
              "RedundantVisibilityModifier",
            )
            public class TestGeneric6__Factory : Factory<TestGeneric6<*, *>> {
              public override fun createInstance(scope: Scope): TestGeneric6<Exception, Any> =
                  TestGeneric6<Exception, Any>()
            
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
            .generatesSources(expectedResult)
    }

    @Test
    fun generic_class_two_type_params_inheriting_one() {
        val source = ktSource(
            "TestGeneric3",
            """
            package test
            import toothpick.InjectConstructor
            
            class GenericBaseClass<T> 
            
            @InjectConstructor
            class TestGeneric3<T, U> : GenericBaseClass<T>
            """
        )

        val expectedResult = expectedKtSource(
            "test/TestGeneric3__Factory",
            """
            package test
            
            import kotlin.Any
            import kotlin.Boolean
            import kotlin.Suppress
            import toothpick.Factory
            import toothpick.Scope

            @Suppress(
              "ClassName",
              "RedundantVisibilityModifier",
            )
            public class TestGeneric3__Factory : Factory<TestGeneric3<*, *>> {
              public override fun createInstance(scope: Scope): TestGeneric3<Any?, Any?> =
                  TestGeneric3<Any?, Any?>()
            
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
            .generatesSources(expectedResult)
    }

    @Test
    fun generic_class_with_bounds_to_generic_class() {
        val source = ktSource(
            "TestGeneric4",
            """
            package test
            import toothpick.InjectConstructor
            
            class GenericClass<T> 
            
            @InjectConstructor
            class TestGeneric4<T> where T : GenericClass<*>
            """
        )

        val expectedResult = expectedKtSource(
            "test/TestGeneric4__Factory",
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
            public class TestGeneric4__Factory : Factory<TestGeneric4<*>> {
              public override fun createInstance(scope: Scope): TestGeneric4<GenericClass<*>> =
                  TestGeneric4<GenericClass<*>>()
            
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
            .generatesSources(expectedResult)
    }

    @Test
    fun generic_class_with_bounds_to_trivial_class() {
        val source = ktSource(
            "TestGeneric5",
            """
            package test
            import java.lang.Exception
            import toothpick.InjectConstructor
            
            
            @InjectConstructor
            class TestGeneric5<T> where T : Exception
            """
        )

        val exptectedResult = expectedKtSource(
            "test/TestGeneric5__Factory",
            """
            package test
            
            import java.lang.Exception
            import kotlin.Boolean
            import kotlin.Suppress
            import toothpick.Factory
            import toothpick.Scope

            @Suppress(
              "ClassName",
              "RedundantVisibilityModifier",
            )
            public class TestGeneric5__Factory : Factory<TestGeneric5<*>> {
              public override fun createInstance(scope: Scope): TestGeneric5<Exception> =
                  TestGeneric5<Exception>()
            
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
            .generatesSources(exptectedResult)
    }

    @Test
    fun member_injection_generic_class() {
        val source = ktSource(
            "TestGenericMemberInjection",
            """
            package test
            import javax.inject.Inject
            import toothpick.InjectConstructor
            
            
            class TestGenericMemberInjection<T> where T: Any {
                 @Inject
                 lateinit var myVar: String
            }
            """
        )

        val expectedResult =
            expectedKtSource(
                "test/TestGenericMemberInjection__Factory",
                """
            package test

            import kotlin.Any
            import kotlin.Boolean
            import kotlin.Suppress
            import toothpick.Factory
            import toothpick.MemberInjector
            import toothpick.Scope

            @Suppress(
              "ClassName",
              "RedundantVisibilityModifier",
            )
            public class TestGenericMemberInjection__Factory : Factory<TestGenericMemberInjection<*>> {
              private val memberInjector: MemberInjector<TestGenericMemberInjection<*>> =
                  TestGenericMemberInjection__MemberInjector()

              @Suppress("NAME_SHADOWING")
              public override fun createInstance(scope: Scope): TestGenericMemberInjection<Any> {
                val scope = getTargetScope(scope)
                return TestGenericMemberInjection<Any>()
                .apply {
                  memberInjector.inject(this, scope)
                }
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
            .generatesSources(expectedResult)
    }

    @Test
    fun `generic class with two constraints shall throw exception`() {
        val source = ktSource(
            name = "test/GenericClassTwoConstraints__Factory",
            contents = """
                package test

                import toothpick.InjectConstructor

                @InjectConstructor
                class GenericClassTwoConstraints<T>(val text: String) where T: java.lang.Exception, T: java.lang.ConstantGroup
            """
        )

        compilationAssert()
            .that(source)
            .processedWith(FactoryProcessorProvider())
            .failsToCompile()
            .withLogContaining(GenericSupportException.TOO_MANY_CONSTRAINTS)
    }
}