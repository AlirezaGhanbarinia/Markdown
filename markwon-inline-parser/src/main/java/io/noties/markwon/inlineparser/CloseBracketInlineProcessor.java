package io.noties.markwon.inlineparser;

import org.commonmark.internal.Bracket;
import org.commonmark.internal.inline.Position;
import org.commonmark.internal.util.Escaping;
import org.commonmark.node.Image;
import org.commonmark.node.Link;
import org.commonmark.node.LinkReferenceDefinition;
import org.commonmark.node.Node;

import java.util.regex.Pattern;

import static io.noties.markwon.inlineparser.InlineParserUtils.mergeChildTextNodes;

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
        Position startIndex = scanner.position();

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

        // Check to see if we have a link/image

        String dest = null;
        String title = null;
        boolean isLinkOrImage = false;

        // Maybe a inline link like `[foo](/uri "title")`
        if (peek() == '(') {
            scanner.next();
            spnl();
            if ((dest = parseLinkDestination()) != null) {
                spnl();
                Position beforeTitle = scanner.position();
                // title needs a whitespace before
                if (scanner.whitespace() > 0) {
                    title = parseLinkTitle();
                    spnl();
                }
                if (peek() == ')') {
                    scanner.next();
                    isLinkOrImage = true;
                } else {
                    scanner.setPosition(startIndex);
                }
            }
        }

        // Maybe a reference link like `[foo][bar]`, `[foo][]` or `[foo]`
        if (!isLinkOrImage) {

            // See if there's a link label like `[bar]` or `[]`
            Position beforeLabel = scanner.position();
            parseLinkLabel();
            Position afterLabel = scanner.position();
            int labelLength = scanner.getSource(beforeLabel, afterLabel).getContent().length();
            String ref = null;
            if (labelLength > 2) {
                ref = scanner.getSource(beforeLabel, afterLabel).getContent();
            } else if (!opener.bracketAfter) {
                // If the second label is empty `[foo][]` or missing `[foo]`, then the first label is the reference.
                // But it can only be a reference when there's no (unescaped) bracket in it.
                // If there is, we don't even need to try to look up the reference. This is an optimization.
                ref = scanner.getSource(opener.contentPosition, startIndex).getContent();
            }

            if (ref != null) {
                String label = normalizeReference(ref);
                LinkReferenceDefinition definition = context.getLinkReferenceDefinition(label);
                if (definition != null) {
                    dest = definition.getDestination();
                    title = definition.getTitle();
                    isLinkOrImage = true;
                }
            }
        }

        if (isLinkOrImage) {
            // If we got here, open is a potential opener
            Node linkOrImage = opener.image ? new Image(dest, title) : new Link(dest, title);

            Node node = opener.node.getNext();
            while (node != null) {
                Node next = node.getNext();
                linkOrImage.appendChild(node);
                node = next;
            }

            // Process delimiters such as emphasis inside link/image
            processDelimiters(opener.previousDelimiter);
            mergeChildTextNodes(linkOrImage);
            // We don't need the corresponding text node anymore, we turned it into a link/image node
            opener.node.unlink();
            removeLastBracket();

            // Links within links are not allowed. We found this link, so there can be no other link around it.
            if (!opener.image) {
                Bracket bracket = lastBracket();
                while (bracket != null) {
                    if (!bracket.image) {
                        // Disallow link opener. It will still get matched, but will not result in a link.
                        bracket.allowed = false;
                    }
                    bracket = bracket.previous;
                }
            }

            return linkOrImage;

        } else { // no link or image
            scanner.setPosition(startIndex);
            removeLastBracket();

            return text("]");
        }
    }

    public static String normalizeReference(String input) {
        // Strip '[' and ']'
        String stripped = input.substring(1, input.length() - 1);
        return Escaping.normalizeLabelContent(stripped);
    }
}
