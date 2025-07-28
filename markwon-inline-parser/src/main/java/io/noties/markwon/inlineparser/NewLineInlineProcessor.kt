package io.noties.markwon.inlineparser

import org.commonmark.node.HardLineBreak
import org.commonmark.node.Node
import org.commonmark.node.SoftLineBreak
import org.commonmark.node.Text
import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * @since 4.2.0
 */
class NewLineInlineProcessor : InlineProcessor() {
    override fun specialCharacter(): Char {
        return '\n'
    }

    override fun parse(): Node? {
        scanner.next() // assume we're at a \n

        val previous = block.lastChild

        // Check previous text for trailing spaces.
        // The "endsWith" is an optimization to avoid an RE match in the common case.
        if (previous is Text && previous.literal.endsWith(" ")) {
            val text = previous
            val literal = text.literal
            val matcher: Matcher = FINAL_SPACE.matcher(literal)
            val spaces = if (matcher.find()) matcher.end() - matcher.start() else 0
            if (spaces > 0) {
                text.literal = literal.substring(0, literal.length - spaces)
            }
            return if (spaces >= 2) {
                HardLineBreak()
            } else {
                SoftLineBreak()
            }
        } else {
            return SoftLineBreak()
        }
    }

    companion object {
        private val FINAL_SPACE: Pattern = Pattern.compile(" *$")
    }
}
