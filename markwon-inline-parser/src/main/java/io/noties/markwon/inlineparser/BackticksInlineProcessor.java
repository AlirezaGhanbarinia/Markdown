package io.noties.markwon.inlineparser;

import org.commonmark.node.Code;
import org.commonmark.node.Node;
import org.commonmark.parser.SourceLines;
import org.commonmark.parser.beta.Position;

/**
 * Parses inline code surrounded with {@code `} chars {@code `code`}
 *
 * @since 4.2.0
 */
public class BackticksInlineProcessor extends InlineProcessor {

    @Override
    public char specialCharacter() {
        return '`';
    }

    @Override
    protected Node parse() {
        int openingTicksCount = scanner.match(c -> c == '`');
        if (openingTicksCount <= 0) return null;

        final Position afterOpen = scanner.position();
        Position beforeClose = null;

        while (scanner.hasNext()) {
            Position preMatch = scanner.position();
            int closingTicksCount = scanner.match(c -> c == '`');

            if (closingTicksCount == openingTicksCount) {
                beforeClose = preMatch;
                break;
            }
        }

        if (beforeClose != null) {
            SourceLines contentLines = scanner.getSource(afterOpen, beforeClose);
            String content = contentLines.getContent().replace('\n', ' ');

            if (content.length() >= 3 &&
                    content.charAt(0) == ' ' &&
                    content.charAt(content.length() - 1) == ' ' &&
                    hasNonSpace(content)) {
                content = content.substring(1, content.length() - 1);
            }

            Code code = new Code();
            code.setLiteral(content);
            return code;
        }

        return text("`".repeat(openingTicksCount));
    }

    private static boolean hasNonSpace(String content) {
        for (int i = 0; i < content.length(); i++) {
            if (content.charAt(i) != ' ') {
                return true;
            }
        }
        return false;
    }
}
