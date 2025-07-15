package io.noties.markwon.inlineparser

import org.commonmark.internal.Bracket
import org.commonmark.internal.Delimiter
import org.commonmark.internal.inline.Position
import org.commonmark.node.LinkReferenceDefinition
import org.commonmark.node.Node
import org.commonmark.node.Text
import org.commonmark.parser.SourceLines
import java.util.regex.Pattern

interface MarkwonInlineParserContext {
    fun block(): Node

    fun input(): SourceLines

    fun index(): Position

    fun setIndex(index: Position)

    fun lastBracket(): Bracket?

    fun lastDelimiter(): Delimiter?

    fun addBracket(bracket: Bracket?)

    fun removeLastBracket()

    fun spnl()

    /**
     * Returns the char at the current input index, or `'\0'` in case there are no more characters.
     */
    fun peek(): Char

    fun match(re: Pattern): String?

    fun text(text: String): Text

    fun text(text: String, beginIndex: Int, endIndex: Int): Text

    fun getLinkReferenceDefinition(label: String?): LinkReferenceDefinition?

    fun parseLinkDestination(): String?

    fun parseLinkTitle(): String?

    fun parseLinkLabel(): Int

    fun processDelimiters(stackBottom: Delimiter?)
}
