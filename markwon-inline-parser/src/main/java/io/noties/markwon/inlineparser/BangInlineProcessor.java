package io.noties.markwon.inlineparser;

import org.commonmark.internal.Bracket;
import org.commonmark.node.Node;
import org.commonmark.node.Text;
import org.commonmark.node.Visitor;
import org.commonmark.parser.beta.Position;

public class BangInlineProcessor extends InlineProcessor {

    @Override
    public char specialCharacter() {
        return '!';
    }

    @Override
    protected Node parse() {
        final Position markerPosition = scanner.position(); // position of '!'
        scanner.next();

        if (peek() == '[') {
            final Position bracketPosition = scanner.position(); // position of '['
            scanner.next();

            final Text markerNode = text("!");
            final Text bracketNode = text("[");

            addBracket(Bracket.withMarker(
                    markerNode,
                    markerPosition,
                    bracketNode,
                    bracketPosition,
                    scanner.position(), // content starts after `[`
                    lastBracket(),
                    lastDelimiter()
            ));

            // return both as a composite node (common trick in custom parsers)
            return new CompositeNode(markerNode, bracketNode);
        }

        return null;
    }

    // helper node to represent the composite `![`
    private static class CompositeNode extends Node {

        CompositeNode(Node... children) {
            for (Node child : children) {
                appendChild(child);
            }
        }

        @Override
        public void accept(Visitor visitor) {
            Node child = getFirstChild();
            while (child != null) {
                Node next = child.getNext();
                child.accept(visitor);
                child = next;
            }
        }
    }
}

