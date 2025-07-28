package io.noties.markwon.inlineparser

import org.commonmark.node.HardLineBreak
import org.commonmark.node.Node

/**
 * @since 4.2.0
 */
class BackslashInlineProcessor : InlineProcessor() {
    override fun specialCharacter(): Char {
        return '\\'
    }

    override fun parse(): Node? {
        scanner.next()
        val next = scanner.peek()
        val node: Node?
        if (next == '\n') {
            scanner.next() // HardLineBreak
            node = HardLineBreak()
        } else if (ESCAPABLE.matcher(next.toString()).matches()) {
            scanner.next() // Escaped character
            node = text(next.toString())
        } else {
            node = text("\\")
        }
        return node
    }

    companion object {
        private val ESCAPABLE = MarkwonInlineParser.ESCAPABLE
    }
}
