package io.noties.markwon.inlineparser;

import org.commonmark.internal.Bracket;
import org.commonmark.internal.inline.Position;
import org.commonmark.node.Node;
import org.commonmark.node.Text;

/**
 * Parses markdown links {@code [link](#href)}
 *
 * @since 4.2.0
 */
public class OpenBracketInlineProcessor extends InlineProcessor {
    @Override
    public char specialCharacter() {
        return '[';
    }

    @Override
    protected Node parse() {
        Position markerPosition = scanner.position();
        scanner.next();
        Text node = text("[");
        Position contentPosition = scanner.position();
        addBracket(Bracket.link(node, markerPosition, contentPosition, lastBracket(), lastDelimiter()));
        return node;
    }
}
