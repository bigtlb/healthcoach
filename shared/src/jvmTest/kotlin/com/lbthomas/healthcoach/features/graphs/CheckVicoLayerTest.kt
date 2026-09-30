package com.lbthomas.healthcoach.features.graphs

import com.patrykandpatrick.vico.compose.cartesian.layer.ColumnCartesianLayer
import kotlin.test.Test

class CheckVicoLayerTest {
    @Test
    fun testVicoLayerProperties() {
        val clazz = ColumnCartesianLayer::class.java
        println("=== ColumnCartesianLayer fields & methods ===")
        for (m in clazz.declaredMethods) {
            println("METHOD: ${m.name}(${m.parameterTypes.joinToString { it.simpleName }}): ${m.returnType.simpleName}")
        }
        for (f in clazz.declaredFields) {
            println("FIELD: ${f.name}: ${f.type.simpleName}")
        }
        val superClazz = clazz.superclass
        println("=== Superclass: ${superClazz?.name} ===")
        if (superClazz != null) {
            for (m in superClazz.declaredMethods) {
                println("SUPER METHOD: ${m.name}(${m.parameterTypes.joinToString { it.simpleName }}): ${m.returnType.simpleName}")
            }
        }
    }
}
