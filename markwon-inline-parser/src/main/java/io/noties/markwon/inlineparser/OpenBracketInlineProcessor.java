package io.noties.markwon.inlineparser;

import org.commonmark.internal.Bracket;
import org.commonmark.node.Node;
import org.commonmark.node.Text;
import org.commonmark.parser.beta.Position;

public class OpenBracketInlineProcessor extends InlineProcessor {

    @Override
    public char specialCharacter() {
        return '[';
    }

    @Override
    protected Node parse() {
        final Position bracketPosition = scanner.position();
        scanner.next();

        final Text bracketNode = text("[");

        addBracket(Bracket.link(
                bracketNode,
                bracketPosition,
                scanner.position(), // content starts after `[`
                lastBracket(),
                lastDelimiter()
        ));

        return bracketNode;
    }
}