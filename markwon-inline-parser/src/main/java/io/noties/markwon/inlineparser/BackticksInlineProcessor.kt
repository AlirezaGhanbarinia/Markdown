package io.noties.markwon.inlineparser

import org.commonmark.node.Code
import org.commonmark.node.Node
import org.commonmark.parser.beta.Position

/**
 * Parses inline code surrounded with `` ` `` chars `` `code` ``
 *
 * @since 4.2.0
 */
class BackticksInlineProcessor : InlineProcessor() {
    override fun specialCharacter(): Char {
        return '`'
    }

    override fun parse(): Node? {
        val openingTicksCount = scanner.match { c: Char -> c == '`' }
        if (openingTicksCount <= 0) return null

        val afterOpen = scanner.position()
        var beforeClose: Position? = null

        while (scanner.hasNext()) {
            val preMatch = scanner.position()
            val closingTicksCount = scanner.match { c: Char -> c == '`' }

            if (closingTicksCount == openingTicksCount) {
                beforeClose = preMatch
                break
            }
        }

        if (beforeClose != null) {
            val contentLines = scanner.getSource(afterOpen, beforeClose)
            var content = contentLines.getContent().replace('\n', ' ')

            if (content.length >= 3 && content[0] == ' ' && content[content.length - 1] == ' ' &&
                hasNonSpace(content)
            ) {
                content = content.substring(1, content.length - 1)
            }

            val code = Code()
            code.literal = content
            return code
        }

        return text("`".repeat(openingTicksCount))
    }

    companion object {
        private fun hasNonSpace(content: String): Boolean {
            for (i in 0..<content.length) {
                if (content[i] != ' ') {
                    return true
                }
            }
            return false
        }
    }
}
