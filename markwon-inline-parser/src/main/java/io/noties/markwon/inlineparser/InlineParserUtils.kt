package io.noties.markwon.inlineparser

import org.commonmark.node.Node
import org.commonmark.node.Text

/**
 * @since 4.2.0
 */
object InlineParserUtils {
    fun mergeTextNodesBetweenExclusive(fromNode: Node, toNode: Node) {
        // No nodes between them
        if (fromNode === toNode || fromNode.next === toNode) {
            return
        }

        mergeTextNodesInclusive(fromNode.next, toNode.previous)
    }

    fun mergeChildTextNodes(node: Node) {
        // No children or just one child node, no need for merging
        if (node.firstChild === node.lastChild) {
            return
        }

        mergeTextNodesInclusive(node.firstChild, node.lastChild)
    }

    fun mergeTextNodesInclusive(fromNode: Node, toNode: Node) {
        var first: Text? = null
        var last: Text? = null
        var length = 0

        var node = fromNode
        while (node != null) {
            if (node is Text) {
                val text = node
                if (first == null) {
                    first = text
                }
                length += text.literal.length
                last = text
            } else {
                mergeIfNeeded(first, last, length)
                first = null
                last = null
                length = 0
            }
            if (node === toNode) {
                break
            }
            node = node.next
        }

        mergeIfNeeded(first, last, length)
    }

    fun mergeIfNeeded(first: Text?, last: Text?, textLength: Int) {
        if (first != null && last != null && first !== last) {
            val sb = StringBuilder(textLength)
            sb.append(first.literal)
            var node = first.next
            val stop = last.next
            while (node !== stop) {
                sb.append((node as Text).literal)
                val unlink: Node = node
                node = node.next
                unlink.unlink()
            }
            val literal = sb.toString()
            first.literal = literal
        }
    }
}
