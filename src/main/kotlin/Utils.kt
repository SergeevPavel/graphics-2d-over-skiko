import java.awt.EventQueue

fun dispatchIfNeeded(block: () -> Unit) {
    if (EventQueue.isDispatchThread()) {
        block()
    } else {
        EventQueue.invokeLater(block)
    }
}