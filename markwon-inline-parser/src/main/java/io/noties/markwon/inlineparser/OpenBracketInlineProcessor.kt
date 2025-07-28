package io.noties.markwon.inlineparser

import org.commonmark.internal.Bracket
import org.commonmark.node.Node

class OpenBracketInlineProcessor : InlineProcessor() {
    override fun specialCharacter(): Char {
        return '['
    }

    override fun parse(): Node {
        val bracketPosition = scanner.position()
        scanner.next()

        val bracketNode = text("[")

        addBracket(
            Bracket.link(
                bracketNode,
                bracketPosition,
                scanner.position(),  // content starts after `[`
                lastBracket(),
                lastDelimiter()
            )
        )

        return bracketNode
    }
}