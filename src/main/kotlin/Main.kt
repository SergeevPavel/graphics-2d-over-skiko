package org.example


//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
fun main() {
    System.setProperty("sun.java2d.metal", "true")
    println("JDK ${System.getProperty("java.version")} (${System.getProperty("java.vendor")}), runtime: ${System.getProperty("java.runtime.name")} ${System.getProperty("java.runtime.version")}")
    swingSample()
//    runSwingMark()
//    SkiaTextureDemo.run(emptyArray())
}