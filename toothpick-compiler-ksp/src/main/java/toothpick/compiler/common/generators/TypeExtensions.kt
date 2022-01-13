package toothpick.compiler.common.generators

import javax.lang.model.element.Element
import javax.lang.model.type.TypeMirror
import javax.lang.model.util.Types

fun TypeMirror.erased(typeUtils: Types): TypeMirror = typeUtils.erasure(this)
fun TypeMirror.asElement(typeUtils: Types): Element? = typeUtils.asElement(this)

inline fun <reified T : Annotation> Element.hasAnnotation(): Boolean =
    getAnnotation(T::class.java) != null
