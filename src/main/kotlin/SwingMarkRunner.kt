package org.example

import org.example.graphics2d.makeUseSkikoGraphics
import javax.swing.JFrame
import kotlin.emptyArray


fun runSwingMark() {
    SwingMark.setFrameFactory {
        val jframe = JFrame("SwingMarks")
        jframe.makeUseSkikoGraphics()
//        jframe.disableDoubleBuffering()
        jframe
    }
    SwingMark.start(emptyArray())
}