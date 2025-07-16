package io.noties.markwon.inlineparser;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.commonmark.internal.Bracket;
import org.commonmark.internal.Delimiter;
import org.commonmark.node.Node;
import org.commonmark.node.Text;
import org.commonmark.parser.SourceLines;
import org.commonmark.parser.beta.Scanner;

import java.util.regex.Pattern;

/**
 * @see AutolinkInlineProcessor
 * @see BackslashInlineProcessor
 * @see BackticksInlineProcessor
 * @see BangInlineProcessor
 * @see CloseBracketInlineProcessor
 * @see EntityInlineProcessor
 * @see HtmlInlineProcessor
 * @see NewLineInlineProcessor
 * @see OpenBracketInlineProcessor
 * @see MarkwonInlineParser.FactoryBuilder#addInlineProcessor(InlineProcessor)
 * @see MarkwonInlineParser.FactoryBuilder#excludeInlineProcessor(Class)
 * @since 4.2.0
 */
public abstract class InlineProcessor {

    /**
     * Special character that triggers parsing attempt
     */
    public abstract char specialCharacter();

    /**
     * @return boolean indicating if parsing succeeded
     */
    @Nullable
    protected abstract Node parse();


    protected MarkwonInlineParserContext context;
    protected Node block;
    protected SourceLines input;
    protected Scanner scanner;

    @Nullable
    public Node parse(@NonNull MarkwonInlineParserContext context) {
        this.context = context;
        this.block = context.block();
        this.input = context.input();
        this.scanner = context.scanner();

        return parse();
    }

    protected Bracket lastBracket() {
        return context.lastBracket();
    }

    protected Delimiter lastDelimiter() {
        return context.lastDelimiter();
    }

    protected void addBracket(Bracket bracket) {
        context.addBracket(bracket);
    }

    protected void removeLastBracket() {
        context.removeLastBracket();
    }

    protected void spnl() {
        context.spnl();
    }

    @Nullable
    protected String match(@NonNull Pattern re) {
        return context.match(re);
    }

    @Nullable
    protected String parseLinkDestination() {
        return context.parseLinkDestination();
    }

    @Nullable
    protected String parseLinkTitle() {
        return context.parseLinkTitle();
    }

    protected int parseLinkLabel() {
        return context.parseLinkLabel();
    }

    protected void processDelimiters(Delimiter stackBottom) {
        context.processDelimiters(stackBottom);
    }

    @NonNull
    protected Text text(@NonNull String text) {
        return context.text(text);
    }

    @NonNull
    protected Text text(@NonNull String text, int start, int end) {
        return context.text(text, start, end);
    }

    protected char peek() {
        return context.peek();
    }
}
