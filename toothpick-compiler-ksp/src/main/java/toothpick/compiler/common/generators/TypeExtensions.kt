package toothpick.compiler.common.generators

import javax.lang.model.type.TypeMirror
import javax.lang.model.util.Types

fun TypeMirror.erased(typeUtils: Types): TypeMirror = typeUtils.erasure(this)
