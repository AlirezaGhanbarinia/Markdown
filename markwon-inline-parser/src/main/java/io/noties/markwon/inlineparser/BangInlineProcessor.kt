package io.noties.markwon.inlineparser

import org.commonmark.internal.Bracket
import org.commonmark.node.Node
import org.commonmark.node.Visitor

class BangInlineProcessor : InlineProcessor() {
    override fun specialCharacter(): Char {
        return '!'
    }

    override fun parse(): Node? {
        val markerPosition = scanner.position() // position of '!'
        scanner.next()

        if (peek() == '[') {
            val bracketPosition = scanner.position() // position of '['
            scanner.next()

            val markerNode = text("!")
            val bracketNode = text("[")

            addBracket(
                Bracket.withMarker(
                    markerNode,
                    markerPosition,
                    bracketNode,
                    bracketPosition,
                    scanner.position(),  // content starts after `[`
                    lastBracket(),
                    lastDelimiter()
                )
            )

            // return both as a composite node (common trick in custom parsers)
            return CompositeNode(markerNode, bracketNode)
        }

        return null
    }

    // helper node to represent the composite `![`
    private class CompositeNode(vararg children: Node) : Node() {
        init {
            for (child in children) {
                appendChild(child)
            }
        }

        override fun accept(visitor: Visitor) {
            var child = firstChild
            while (child != null) {
                val next = child.next
                child.accept(visitor)
                child = next
            }
        }
    }
}

