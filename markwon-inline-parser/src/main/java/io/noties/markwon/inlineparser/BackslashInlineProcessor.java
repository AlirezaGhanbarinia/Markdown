package io.noties.markwon.inlineparser;

import org.commonmark.node.HardLineBreak;
import org.commonmark.node.Node;

import java.util.regex.Pattern;

/**
 * @since 4.2.0
 */
public class BackslashInlineProcessor extends InlineProcessor {

    private static final Pattern ESCAPABLE = MarkwonInlineParser.ESCAPABLE;

    @Override
    public char specialCharacter() {
        return '\\';
    }

    @Override
    protected Node parse() {
        scanner.next();
        char next = scanner.peek();
        Node node;
        if (next == '\n') {
            scanner.next(); // HardLineBreak
            node = new HardLineBreak();
        } else if (ESCAPABLE.matcher(String.valueOf(next)).matches()) {
            scanner.next(); // Escaped character
            node = text(String.valueOf(next));
        } else {
            node = text("\\");
        }
        return node;
    }
}
