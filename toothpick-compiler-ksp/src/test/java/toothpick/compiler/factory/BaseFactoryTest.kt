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
import com.google.testing.compile.JavaSourceSubjectFactory
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Assert.fail
import javax.tools.JavaFileObject
import javax.tools.StandardLocation

open class BaseFactoryTest {

    protected fun assertThatCompileWithoutErrorButNoFactoryIsCreated(
        source: JavaFileObject?,
        noFactoryPackageName: String?,
        noFactoryClass: String
    ) {
        try {
            Truth.assert_()
                .about(JavaSourceSubjectFactory.javaSource())
                .that(source)
                .processedWith(ProcessorTestUtilities.factoryAndMemberInjectorProcessors())
                .compilesWithoutError()
                .and()
                .generatesFileNamed(
                    StandardLocation.locationFor("CLASS_OUTPUT"),
                    "test",
                    noFactoryClass + "__Factory.class"
                )

            fail("A factory was created when it shouldn't.")
        } catch (e: AssertionError) {
            assertThat(
                e.message,
                CoreMatchers.containsString(
                    "generated the file named \"%s__Factory.class\" in package \"%s\";"
                        .format(noFactoryClass, noFactoryPackageName)
                )
            )
        }
    }
}