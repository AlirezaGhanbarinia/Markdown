package io.noties.markwon.inlineparser;

import org.commonmark.internal.Bracket;
import org.commonmark.internal.inline.Position;
import org.commonmark.node.Node;
import org.commonmark.node.Text;

/**
 * Parses markdown images {@code ![alt](#href)}
 *
 * @since 4.2.0
 */
public class BangInlineProcessor extends InlineProcessor {
    @Override
    public char specialCharacter() {
        return '!';
    }

    @Override
    protected Node parse() {
        Position startPosition = scanner.position();
        if (!scanner.next('[')) {
            return null;
        }
        Text node = text("![");
        Position contentPosition = scanner.position();
        addBracket(Bracket.image(node, startPosition, contentPosition, lastBracket(), lastDelimiter()));
        return node;
    }
}
