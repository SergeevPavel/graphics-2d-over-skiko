package org.example

import org.example.graphics2d.makeUseSkikoGraphics1
import javax.swing.JFrame
import kotlin.emptyArray


fun runSwingMark() {
    SwingMark.setFrameFactory {
        val jframe = JFrame("SwingMarks")
        jframe.makeUseSkikoGraphics1()
//        jframe.disableDoubleBuffering()
        jframe
    }
    SwingMark.start(emptyArray())
}