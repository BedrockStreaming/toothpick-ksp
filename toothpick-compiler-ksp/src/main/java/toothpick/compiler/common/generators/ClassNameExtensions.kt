package toothpick.compiler.common.generators

import javax.lang.model.element.ElementKind
import javax.lang.model.element.TypeElement

internal val TypeElement.generatedFQNClassName: String
    get() = "$generatedPackageName.$generatedSimpleClassName"

internal val TypeElement.generatedSimpleClassName: String
    get() {
        var currentTypeElement = this
        var result = currentTypeElement.simpleName.toString()
        // deals with inner classes
        while (currentTypeElement.enclosingElement.kind != ElementKind.PACKAGE) {
            result = "${currentTypeElement.enclosingElement.simpleName}$$result"
            currentTypeElement = currentTypeElement.enclosingElement as TypeElement
        }
        return result
    }

internal val TypeElement.generatedPackageName: String
    get() {
        // deals with inner classes
        var currentTypeElement = this
        while (currentTypeElement.enclosingElement.kind != ElementKind.PACKAGE) {
            currentTypeElement = currentTypeElement.enclosingElement as TypeElement
        }
        return currentTypeElement.enclosingElement.toString()
    }
