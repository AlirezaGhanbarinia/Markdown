package io.noties.markwon.inlineparser;

import static io.noties.markwon.inlineparser.InlineParserUtils.mergeChildTextNodes;

import org.commonmark.internal.Bracket;
import org.commonmark.internal.util.Escaping;
import org.commonmark.node.Image;
import org.commonmark.node.Link;
import org.commonmark.node.LinkReferenceDefinition;
import org.commonmark.node.Node;
import org.commonmark.parser.beta.Position;

/**
 * Parses markdown link or image, relies on {@link OpenBracketInlineProcessor}
 * to handle start of these elements
 *
 * @since 4.2.0
 */
public class CloseBracketInlineProcessor extends InlineProcessor {

    @Override
    public char specialCharacter() {
        return ']';
    }

    @Override
    protected Node parse() {
        scanner.next();
        Position closeBracketPosition = scanner.position();

        // Get previous `[` or `![`
        Bracket opener = lastBracket();
        if (opener == null) {
            // No matching opener, just return a literal.
            return text("]");
        }

        if (!opener.allowed) {
            // Matching opener but it's not allowed, just return a literal.
            removeLastBracket();
            return text("]");
        }
        boolean isImage = opener.markerNode != null;

        String destination = null;
        String title = null;
        boolean isLinkOrImage = false;

        // [text](dest "title")
        if (peek() == '(') {
            scanner.next();
            spnl();

            destination = parseLinkDestination();
            if (destination != null) {
                spnl();
                if (scanner.whitespace() > 0) {
                    title = parseLinkTitle();
                    spnl();
                }
                if (peek() == ')') {
                    scanner.next();
                    isLinkOrImage = true;
                } else {
                    scanner.setPosition(closeBracketPosition);
                }
            }
        }

        // [text][label] یا [text][] یا [text]
        if (!isLinkOrImage) {
            Position beforeLabel = scanner.position();
            parseLinkLabel();
            Position afterLabel = scanner.position();

            int labelLength = scanner.getSource(beforeLabel, afterLabel).getContent().length();
            String reference = null;

            if (labelLength > 2) {
                reference = scanner.getSource(beforeLabel, afterLabel).getContent();
            } else if (!opener.bracketAfter) {
                reference = scanner.getSource(opener.contentPosition, closeBracketPosition).getContent();
            }

            if (reference != null) {
                String normalized = normalizeReference(reference);
                LinkReferenceDefinition def = context.getLinkReferenceDefinition(normalized);
                if (def != null) {
                    destination = def.getDestination();
                    title = def.getTitle();
                    isLinkOrImage = true;
                }
            }
        }

        if (isLinkOrImage) {
            Node linkOrImage = isImage ? new Image(destination, title) : new Link(destination, title);

            Node child = opener.bracketNode.getNext();
            while (child != null && child != opener.bracketNode) {
                Node next = child.getNext();
                linkOrImage.appendChild(child);
                child = next;
            }

            processDelimiters(opener.previousDelimiter);
            mergeChildTextNodes(linkOrImage);

            opener.bracketNode.unlink();
            if (isImage && opener.markerNode != null) {
                opener.markerNode.unlink();
            }
            removeLastBracket();

            // Links داخل لینک ممنوعه
            if (!isImage) {
                Bracket bracket = lastBracket();
                while (bracket != null) {
                    if (bracket.markerNode == null) {
                        bracket.allowed = false;
                    }
                    bracket = bracket.previous;
                }
            }

            return linkOrImage;
        } else {
            scanner.setPosition(closeBracketPosition);
            removeLastBracket();
            return text("]");
        }
    }

    public static String normalizeReference(String input) {
        // "[label]" -> "label"
        String stripped = input.substring(1, input.length() - 1);
        return Escaping.normalizeLabelContent(stripped);
    }
}
