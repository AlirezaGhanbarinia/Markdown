package io.noties.markwon.inlineparser

import org.commonmark.internal.Bracket
import org.commonmark.internal.Delimiter
import org.commonmark.node.Node
import org.commonmark.node.Text
import org.commonmark.parser.SourceLines
import org.commonmark.parser.beta.Scanner
import java.util.regex.Pattern

/**
 * @see AutolinkInlineProcessor
 *
 * @see BackslashInlineProcessor
 *
 * @see BackticksInlineProcessor
 *
 * @see BangInlineProcessor
 *
 * @see CloseBracketInlineProcessor
 *
 * @see EntityInlineProcessor
 *
 * @see HtmlInlineProcessor
 *
 * @see NewLineInlineProcessor
 *
 * @see OpenBracketInlineProcessor
 *
 * @see MarkwonInlineParser.FactoryBuilder.addInlineProcessor
 * @see MarkwonInlineParser.FactoryBuilder.excludeInlineProcessor
 * @since 4.2.0
 */
abstract class InlineProcessor {
    /**
     * Special character that triggers parsing attempt
     */
    abstract fun specialCharacter(): Char

    /**
     * @return boolean indicating if parsing succeeded
     */
    protected abstract fun parse(): Node?


    protected lateinit var context: MarkwonInlineParserContext
    protected lateinit var block: Node
    protected lateinit var scanner: Scanner

    fun parse(context: MarkwonInlineParserContext): Node? {
        this.context = context
        this.block = context.block()
        this.scanner = context.scanner()

        return parse()
    }

    protected fun lastBracket(): Bracket? {
        return context.lastBracket()
    }

    protected fun lastDelimiter(): Delimiter? {
        return context.lastDelimiter()
    }

    protected fun addBracket(bracket: Bracket) {
        context.addBracket(bracket)
    }

    protected fun removeLastBracket() {
        context.removeLastBracket()
    }

    protected fun spnl() {
        context.spnl()
    }

    protected fun match(re: Pattern): String? {
        return context.match(re)
    }

    protected fun parseLinkDestination(): String? {
        return context.parseLinkDestination()
    }

    protected fun parseLinkTitle(): String? {
        return context.parseLinkTitle()
    }

    protected fun parseLinkLabel(): Int {
        return context.parseLinkLabel()
    }

    protected fun processDelimiters(stackBottom: Delimiter?) {
        context.processDelimiters(stackBottom)
    }

    protected fun text(text: String): Text {
        return context.text(text)
    }

    protected fun text(text: String, start: Int, end: Int): Text {
        return context.text(text, start, end)
    }

    protected fun peek(): Char {
        return context.peek()
    }
}
