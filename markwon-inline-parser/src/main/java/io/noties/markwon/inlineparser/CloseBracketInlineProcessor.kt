package io.noties.markwon.inlineparser

import org.commonmark.internal.util.Escaping
import org.commonmark.node.Image
import org.commonmark.node.Link
import org.commonmark.node.Node

/**
 * Parses markdown link or image, relies on [OpenBracketInlineProcessor]
 * to handle start of these elements
 *
 * @since 4.2.0
 */
class CloseBracketInlineProcessor : InlineProcessor() {
    override fun specialCharacter(): Char {
        return ']'
    }

    override fun parse(): Node? {
        scanner.next()
        val closeBracketPosition = scanner.position()

        // Get previous `[` or `![`
        val opener = lastBracket()
        if (opener == null) {
            // No matching opener, just return a literal.
            return text("]")
        }

        if (!opener.allowed) {
            // Matching opener but it's not allowed, just return a literal.
            removeLastBracket()
            return text("]")
        }
        val isImage = opener.markerNode != null

        var destination: String? = null
        var title: String? = null
        var isLinkOrImage = false

        // [text](dest "title")
        if (peek() == '(') {
            scanner.next()
            spnl()

            destination = parseLinkDestination()
            if (destination != null) {
                spnl()
                if (scanner.whitespace() > 0) {
                    title = parseLinkTitle()
                    spnl()
                }
                if (peek() == ')') {
                    scanner.next()
                    isLinkOrImage = true
                } else {
                    scanner.setPosition(closeBracketPosition)
                }
            }
        }

        // [text][label] یا [text][] یا [text]
        if (!isLinkOrImage) {
            val beforeLabel = scanner.position()
            parseLinkLabel()
            val afterLabel = scanner.position()

            val labelLength = scanner.getSource(beforeLabel, afterLabel).getContent().length
            var reference: String? = null

            if (labelLength > 2) {
                reference = scanner.getSource(beforeLabel, afterLabel).getContent()
            } else if (!opener.bracketAfter) {
                reference = scanner.getSource(opener.contentPosition, closeBracketPosition).getContent()
            }

            if (reference != null) {
                val normalized: String? = normalizeReference(reference)
                val def = context.getLinkReferenceDefinition(normalized)
                if (def != null) {
                    destination = def.destination
                    title = def.title
                    isLinkOrImage = true
                }
            }
        }

        if (isLinkOrImage) {
            val linkOrImage = if (isImage) Image(destination, title) else Link(destination, title)

            var child = opener.bracketNode.next
            while (child != null && child !== opener.bracketNode) {
                val next = child.next
                linkOrImage.appendChild(child)
                child = next
            }

            processDelimiters(opener.previousDelimiter)
            InlineParserUtils.mergeChildTextNodes(linkOrImage)

            opener.bracketNode.unlink()
            if (isImage) {
                opener.markerNode.unlink()
            }
            removeLastBracket()

            // Links داخل لینک ممنوعه
            if (!isImage) {
                var bracket = lastBracket()
                while (bracket != null) {
                    if (bracket.markerNode == null) {
                        bracket.allowed = false
                    }
                    bracket = bracket.previous
                }
            }

            return linkOrImage
        } else {
            scanner.setPosition(closeBracketPosition)
            removeLastBracket()
            return text("]")
        }
    }

    companion object {
        fun normalizeReference(input: String): String? {
            // "[label]" -> "label"
            val stripped = input.substring(1, input.length - 1)
            return Escaping.normalizeLabelContent(stripped)
        }
    }
}
