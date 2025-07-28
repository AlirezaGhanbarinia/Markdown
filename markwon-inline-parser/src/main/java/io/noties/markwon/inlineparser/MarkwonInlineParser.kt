package io.noties.markwon.inlineparser

import org.commonmark.internal.Bracket
import org.commonmark.internal.Delimiter
import org.commonmark.internal.inline.AsteriskDelimiterProcessor
import org.commonmark.internal.inline.UnderscoreDelimiterProcessor
import org.commonmark.internal.util.Escaping
import org.commonmark.internal.util.LinkScanner
import org.commonmark.node.LinkReferenceDefinition
import org.commonmark.node.Node
import org.commonmark.node.Text
import org.commonmark.parser.InlineParser
import org.commonmark.parser.InlineParserContext
import org.commonmark.parser.InlineParserFactory
import org.commonmark.parser.SourceLines
import org.commonmark.parser.beta.Scanner
import org.commonmark.parser.delimiter.DelimiterProcessor
import java.util.BitSet
import java.util.regex.Pattern

/**
 * @see .factoryBuilder
 * @see .factoryBuilderNoDefaults
 * @see FactoryBuilder
 *
 * @since 4.2.0
 */
class MarkwonInlineParser(
    private val inlineParserContext: InlineParserContext,
    private val referencesEnabled: Boolean,
    inlineProcessors: MutableList<InlineProcessor>,
    delimiterProcessors: MutableList<DelimiterProcessor>
) : InlineParser, MarkwonInlineParserContext {

    @Suppress("unused")
    interface FactoryBuilder {
        /**
         * @see InlineProcessor
         */
        fun addInlineProcessor(processor: InlineProcessor): FactoryBuilder

        /**
         * @see AsteriskDelimiterProcessor
         *
         * @see UnderscoreDelimiterProcessor
         */
        fun addDelimiterProcessor(processor: DelimiterProcessor): FactoryBuilder

        /**
         * Indicate if markdown references are enabled. By default = `true`
         */
        fun referencesEnabled(referencesEnabled: Boolean): FactoryBuilder

        fun excludeInlineProcessor(processor: Class<out InlineProcessor?>): FactoryBuilder

        fun excludeDelimiterProcessor(processor: Class<out DelimiterProcessor?>): FactoryBuilder

        fun build(): InlineParserFactory
    }

    interface FactoryBuilderNoDefaults : FactoryBuilder {
        /**
         * Includes all default delimiter and inline processors, and sets `referencesEnabled=true`.
         * Useful with subsequent calls to [.excludeInlineProcessor] or [.excludeDelimiterProcessor]
         */
        fun includeDefaults(): FactoryBuilder
    }

    private val specialCharacters: BitSet
    private val inlineProcessors: MutableMap<Char, MutableList<InlineProcessor>> = calculateInlines(inlineProcessors)
    private val delimiterProcessors: MutableMap<Char, DelimiterProcessor>

    // currently we still hold a reference to it because we decided not to
    //  pass previous node argument to inline-processors (current usage is limited with NewLineInlineProcessor)
    private lateinit var block: Node
    private lateinit var input: SourceLines
    private lateinit var scanner: Scanner

    /**
     * Top delimiter (emphasis, strong emphasis or custom emphasis). (Brackets are on a separate stack, different
     * from the algorithm described in the spec.)
     */
    private var lastDelimiter: Delimiter? = null

    /**
     * Top opening bracket (`[` or `![)`).
     */
    private var lastBracket: Bracket? = null

    // might we construct these in factory?
    init {
        this.delimiterProcessors = calculateDelimiterProcessors(delimiterProcessors)
        this.specialCharacters = calculateSpecialCharacters(
            this.inlineProcessors.keys,
            this.delimiterProcessors.keys
        )
    }

    /**
     * Parse content in block into inline children, using reference map to resolve references.
     */
    override fun parse(content: SourceLines, block: Node) {
        reset(content)

        // we still reference it
        this.block = block

        while (true) {
            val node = parseInline()
            if (node != null) {
                block.appendChild(node)
            } else {
                break
            }
        }

        processDelimiters(null)
        InlineParserUtils.mergeChildTextNodes(block)
    }

    private fun reset(content: SourceLines) {
        this.input = content
        this.scanner = Scanner.of(content)
        this.lastDelimiter = null
        this.lastBracket = null
    }

    /**
     * Parse the next inline element in subject, advancing input index.
     * On success, add the result to block's children and return true.
     * On failure, return false.
     */
    private fun parseInline(): Node? {
        val c = peek()

        if (c == Scanner.END) {
            return null
        }

        var node: Node? = null

        val inlines = this.inlineProcessors[c]

        if (inlines != null) {
            val pos = scanner.position()

            for (inline in inlines) {
                node = inline.parse(this)
                if (node != null) {
                    break
                }

                // reset after each iteration (happens only when node is null)
                scanner.setPosition(pos)
            }
        } else {
            val delimiterProcessor = delimiterProcessors[c]
            node = if (delimiterProcessor != null) {
                parseDelimiters(delimiterProcessor, c)
            } else {
                parseString()
            }
        }

        if (node != null) {
            return node
        } else {
            scanner.next()
            val literal = c.toString()
            return text(literal)
        }
    }

    /**
     * If RE matches at current index in the input, advance index and return the match; otherwise return null.
     */
    override fun match(re: Pattern): String? {
        val start = scanner.position()

        if (!scanner.hasNext()) {
            return null
        }

        val remaining = StringBuilder()
        while (scanner.hasNext()) {
            remaining.append(scanner.peek())
            scanner.next()
        }

        scanner.setPosition(start)

        val matcher = re.matcher(remaining)
        if (matcher.lookingAt()) {
            val matched = matcher.group()

            repeat(matched.length) {
                scanner.next()
            }

            return matched
        }
        return null
    }

    override fun text(text: String): Text {
        return Text(text)
    }

    override fun text(text: String, beginIndex: Int, endIndex: Int): Text {
        return Text(text.substring(beginIndex, endIndex))
    }

    override fun getLinkReferenceDefinition(label: String?): LinkReferenceDefinition? {
        return if (referencesEnabled) inlineParserContext.getLinkReferenceDefinition(label) else null
    }

    /**
     * Returns the char at the current input index, or `'\0'` in case there are no more characters.
     */
    override fun peek(): Char {
        return scanner.peek()
    }

    override fun block(): Node {
        return block
    }

    override fun input(): SourceLines {
        return input
    }

    override fun scanner(): Scanner {
        return scanner
    }

    override fun lastBracket(): Bracket? {
        return lastBracket
    }

    override fun lastDelimiter(): Delimiter? {
        return lastDelimiter
    }

    override fun addBracket(bracket: Bracket?) {
        lastBracket?.bracketAfter = true
        lastBracket = bracket
    }

    override fun removeLastBracket() {
        lastBracket = lastBracket?.previous
    }

    /**
     * Parse zero or more space characters, including at most one newline.
     */
    override fun spnl() {
        match(SPNL)
    }

    /**
     * Attempt to parse delimiters like emphasis, strong emphasis or custom delimiters.
     */
    private fun parseDelimiters(delimiterProcessor: DelimiterProcessor, delimiterChar: Char): Node? {
        val res = scanDelimiters(delimiterProcessor, delimiterChar)
        if (res == null) {
            return null
        }

        val count = res.count
        val start = scanner.position()

        repeat(count) {
            scanner.next()
        }
        val end = scanner.position()
        val literal = scanner.getSource(start, end).getContent()
        val node = Text(literal)
        val delimiter = Delimiter(mutableListOf<Text?>(node), delimiterChar, res.canOpen, res.canClose, lastDelimiter)
        lastDelimiter?.next = delimiter
        lastDelimiter = delimiter
        return node
    }


    /**
     * Attempt to parse link destination, returning the string or null if no match.
     */
    override fun parseLinkDestination(): String? {
        val start = scanner.position()
        if (!LinkScanner.scanLinkDestination(scanner)) {
            return null
        }

        val end = scanner.position()

        val sourceLines = scanner.getSource(start, end)
        var dest = sourceLines.getContent()

        if (dest.length >= 2 && dest[0] == '<' && dest[dest.length - 1] == '>') {
            dest = dest.substring(1, dest.length - 1)
        }

        return Escaping.unescapeString(dest)
    }

    /**
     * Attempt to parse link title (sans quotes), returning the string or null if no match.
     */
    override fun parseLinkTitle(): String? {
        val start = scanner.position()
        if (!LinkScanner.scanLinkTitle(scanner)) {
            return null
        }

        val end = scanner.position()

        val lines = scanner.getSource(start, end)
        var title = lines.getContent()

        // chop off ', " or parens
        if (title.length >= 2) {
            title = title.substring(1, title.length - 1)
        } else {
            return null
        }
        return Escaping.unescapeString(title)
    }

    /**
     * Attempt to parse a link label, returning number of characters parsed.
     */
    override fun parseLinkLabel(): Int {
        if (!scanner.hasNext() || scanner.peek() != '[') {
            return 0
        }

        val start = scanner.position()

        scanner.next()
        val contentStart = scanner.position()

        if (!LinkScanner.scanLinkLabelContent(scanner)) {
            return 0
        }

        if (!scanner.hasNext() || scanner.peek() != ']') {
            return 0
        }

        val contentEnd = scanner.position()

        // spec: A link label can have at most 999 characters inside the square brackets.
        val labelContentLines = scanner.getSource(contentStart, contentEnd)
        val labelContent = labelContentLines.getContent()
        if (labelContent.length > 999) {
            return 0
        }

        scanner.next()

        val end = scanner.position()
        val wholeLabelLines = scanner.getSource(start, end)
        return wholeLabelLines.getContent().length
    }

    /**
     * Parse a run of ordinary characters, or a single character with a special meaning in markdown, as a plain string.
     */
    private fun parseString(): Node? {
        val sb = buildString {
            while (scanner.hasNext()) {
                val c = scanner.peek()
                if (specialCharacters.get(c.code)) {
                    break
                }
                append(c)
                scanner.next()
            }
        }

        return if (sb.isEmpty()) {
            null
        } else {
            Text(sb)
        }
    }

    /**
     * Scan a sequence of characters with code delimiterChar, and return information about the number of delimiters
     * and whether they are positioned such that they can open and/or close emphasis or strong emphasis.
     *
     * @return information about delimiter run, or `null`
     */
    private fun scanDelimiters(delimiterProcessor: DelimiterProcessor, delimiterChar: Char): DelimiterData? {
        val start = scanner.position()

        var delimiterCount = 0
        while (peek() == delimiterChar) {
            delimiterCount++
            scanner.next()
        }

        if (delimiterCount < delimiterProcessor.minLength) {
            scanner.setPosition(start)
            return null
        }

        val before: String?
        val codePointBefore = scanner.peekPreviousCodePoint()
        before = if (codePointBefore == Scanner.END.code) {
            "\n"
        } else {
            codePointBefore.toChar().toString()
        }

        val charAfter = if (scanner.hasNext()) scanner.peek() else Scanner.END
        val after = if (charAfter == Scanner.END) "\n" else charAfter.toString()

        // We could be more lazy here, in most cases we don't need to do every match case.
        val beforeIsPunctuation = PUNCTUATION.matcher(before).matches()
        val beforeIsWhitespace = UNICODE_WHITESPACE_CHAR.matcher(before).matches()
        val afterIsPunctuation = PUNCTUATION.matcher(after).matches()
        val afterIsWhitespace = UNICODE_WHITESPACE_CHAR.matcher(after).matches()

        val leftFlanking = !afterIsWhitespace && (!afterIsPunctuation || beforeIsWhitespace || beforeIsPunctuation)
        val rightFlanking = !beforeIsWhitespace && (!beforeIsPunctuation || afterIsWhitespace || afterIsPunctuation)
        val canOpen: Boolean
        val canClose: Boolean
        if (delimiterChar == '_') {
            canOpen = leftFlanking && (!rightFlanking || beforeIsPunctuation)
            canClose = rightFlanking && (!leftFlanking || afterIsPunctuation)
        } else {
            canOpen = leftFlanking && delimiterChar == delimiterProcessor.openingCharacter
            canClose = rightFlanking && delimiterChar == delimiterProcessor.closingCharacter
        }

        scanner.setPosition(start)
        return DelimiterData(delimiterCount, canOpen, canClose)
    }

    override fun processDelimiters(stackBottom: Delimiter?) {
        val openersBottom: MutableMap<Char?, Delimiter?> = HashMap()

        // find first closer above stackBottom:
        var closer = lastDelimiter
        while (closer != null && closer.previous !== stackBottom) {
            closer = closer.previous
        }
        // move forward, looking for closers, and handling each
        while (closer != null) {
            val delimiterChar = closer.delimiterChar

            val delimiterProcessor = delimiterProcessors[delimiterChar]
            if (!closer.canClose() || delimiterProcessor == null) {
                closer = closer.next
                continue
            }

            val openingDelimiterChar = delimiterProcessor.openingCharacter

            // Found delimiter closer. Now look back for first matching opener.
            var openerFound = false
            var potentialOpenerFound = false
            var opener = closer.previous
            while (opener != null && opener !== stackBottom && opener !== openersBottom[delimiterChar]) {
                if (opener.canOpen() && opener.delimiterChar == openingDelimiterChar) {
                    potentialOpenerFound = true
                    val useDelims = delimiterProcessor.process(opener, closer)
                    if (useDelims > 0) {
                        openerFound = true
                        break
                    }
                }
                opener = opener.previous
            }

            if (!openerFound) {
                if (!potentialOpenerFound) {
                    // Set lower bound for future searches for openers.
                    // Only do this when we didn't even have a potential
                    // opener (one that matches the character and can open).
                    // If an opener was rejected because of the number of
                    // delimiters (e.g. because of the "multiple of 3" rule),
                    // we want to consider it next time because the number
                    // of delimiters can change as we continue processing.
                    openersBottom.put(delimiterChar, closer.previous)
                    if (!closer.canOpen()) {
                        // We can remove a closer that can't be an opener,
                        // once we've seen there's no matching opener:
                        removeDelimiterKeepNode(closer)
                    }
                }
                closer = closer.next
                continue
            }

            val useDelims = delimiterProcessor.process(opener, closer)
            if (useDelims <= 0) {
                closer = closer.next
                continue
            }

            repeat(useDelims) {
                val t = opener.characters.removeAt(opener.characters.size - 1)
                removeNodeIfEmpty(t)
            }
            repeat(useDelims) {
                val t = closer.characters.removeAt(0)
                removeNodeIfEmpty(t)
            }

            removeDelimitersBetween(opener, closer)

            if (opener.length() == 0) {
                removeDelimiterAndNode(opener)
            }

            if (closer.length() == 0) {
                val next = closer.next
                removeDelimiterAndNode(closer)
                closer = next
            } else {
                closer = closer.next
            }
        }

        // remove all delimiters
        while (lastDelimiter != null && lastDelimiter !== stackBottom) {
            removeDelimiterKeepNode(lastDelimiter)
        }
    }

    private fun removeNodeIfEmpty(text: Text) {
        if (text.literal.isEmpty()) {
            text.unlink()
        }
    }

    private fun removeDelimitersBetween(opener: Delimiter?, closer: Delimiter) {
        var delimiter = closer.previous
        while (delimiter != null && delimiter !== opener) {
            val previousDelimiter = delimiter.previous
            removeDelimiterKeepNode(delimiter)
            delimiter = previousDelimiter
        }
    }

    /**
     * Remove the delimiter and the corresponding text node. For used delimiters, e.g. `*` in `*foo*`.
     */
    private fun removeDelimiterAndNode(delim: Delimiter) {
        for (text in delim.characters) {
            text.unlink()
        }
        removeDelimiter(delim)
    }

    /**
     * Remove the delimiter but keep the corresponding node as text. For unused delimiters such as `_` in `foo_bar`.
     */
    private fun removeDelimiterKeepNode(delim: Delimiter?) {
        delim?.let {
            removeDelimiter(it)
        }
    }

    private fun removeDelimiter(delim: Delimiter) {
        if (delim.previous != null) {
            delim.previous.next = delim.next
        }
        if (delim.next == null) {
            // top of stack
            lastDelimiter = delim.previous
        } else {
            delim.next.previous = delim.previous
        }
    }

    private class DelimiterData(val count: Int, val canOpen: Boolean, val canClose: Boolean)

    internal class FactoryBuilderImpl : FactoryBuilder, FactoryBuilderNoDefaults {
        private val inlineProcessors: MutableList<InlineProcessor> = ArrayList(3)
        private val delimiterProcessors: MutableList<DelimiterProcessor> = ArrayList(3)
        private var referencesEnabled = false

        override fun addInlineProcessor(processor: InlineProcessor): FactoryBuilder {
            this.inlineProcessors.add(processor)
            return this
        }

        override fun addDelimiterProcessor(processor: DelimiterProcessor): FactoryBuilder {
            this.delimiterProcessors.add(processor)
            return this
        }

        override fun referencesEnabled(referencesEnabled: Boolean): FactoryBuilder {
            this.referencesEnabled = referencesEnabled
            return this
        }

        override fun includeDefaults(): FactoryBuilder {
            // by default enabled

            this.referencesEnabled = true

            this.inlineProcessors.addAll(
                listOf(
                    AutolinkInlineProcessor(),
                    BackslashInlineProcessor(),
                    BackticksInlineProcessor(),
                    BangInlineProcessor(),
                    CloseBracketInlineProcessor(),
                    EntityInlineProcessor(),
                    HtmlInlineProcessor(),
                    NewLineInlineProcessor(),
                    OpenBracketInlineProcessor()
                )
            )

            this.delimiterProcessors.addAll(listOf(AsteriskDelimiterProcessor(), UnderscoreDelimiterProcessor()))

            return this
        }

        override fun excludeInlineProcessor(processor: Class<out InlineProcessor?>): FactoryBuilder {
            var i = 0
            val size = inlineProcessors.size
            while (i < size) {
                if (processor == inlineProcessors[i].javaClass) {
                    inlineProcessors.removeAt(i)
                    break
                }
                i++
            }
            return this
        }

        override fun excludeDelimiterProcessor(processor: Class<out DelimiterProcessor?>): FactoryBuilder {
            var i = 0
            val size = delimiterProcessors.size
            while (i < size) {
                if (processor == delimiterProcessors[i].javaClass) {
                    delimiterProcessors.removeAt(i)
                    break
                }
                i++
            }
            return this
        }

        override fun build(): InlineParserFactory {
            return InlineParserFactoryImpl(referencesEnabled, inlineProcessors, delimiterProcessors)
        }
    }

    internal class InlineParserFactoryImpl(
        private val referencesEnabled: Boolean,
        private val inlineProcessors: MutableList<InlineProcessor>,
        private val delimiterProcessors: MutableList<DelimiterProcessor>
    ) : InlineParserFactory {
        override fun create(inlineParserContext: InlineParserContext): InlineParser {
            val delimiterProcessors: MutableList<DelimiterProcessor>
            val customDelimiterProcessors = inlineParserContext.customDelimiterProcessors
            val size = customDelimiterProcessors?.size ?: 0
            if (size > 0) {
                delimiterProcessors = ArrayList(size + this.delimiterProcessors.size)
                delimiterProcessors.addAll(this.delimiterProcessors)
                delimiterProcessors.addAll(customDelimiterProcessors)
            } else {
                delimiterProcessors = this.delimiterProcessors
            }
            return MarkwonInlineParser(
                inlineParserContext,
                referencesEnabled,
                inlineProcessors,
                delimiterProcessors
            )
        }
    }

    companion object {
        /**
         * Creates an instance of [FactoryBuilder] and includes all defaults.
         *
         * @see .factoryBuilderNoDefaults
         */
        @JvmStatic
        fun factoryBuilder(): FactoryBuilder {
            return FactoryBuilderImpl().includeDefaults()
        }

        /**
         * NB, this return an *empty* builder, so if no [FactoryBuilderNoDefaults.includeDefaults]
         * is called, it means effectively **no inline parsing** (unless further calls
         * to [FactoryBuilder.addInlineProcessor] or [FactoryBuilder.addDelimiterProcessor]).
         */
        @JvmStatic
        fun factoryBuilderNoDefaults(): FactoryBuilderNoDefaults {
            return FactoryBuilderImpl()
        }

        private const val ASCII_PUNCTUATION = "!\"#\\$%&'\\(\\)\\*\\+,\\-\\./:;<=>\\?@\\[\\\\\\]\\^_`\\{\\|\\}~"
        private val PUNCTUATION: Pattern =
            Pattern.compile("^[$ASCII_PUNCTUATION\\p{Pc}\\p{Pd}\\p{Pe}\\p{Pf}\\p{Pi}\\p{Po}\\p{Ps}]")

        private val SPNL: Pattern = Pattern.compile("^ *(?:\n *)?")

        private val UNICODE_WHITESPACE_CHAR: Pattern = Pattern.compile("^[\\p{Zs}\t\r\n\u000c]")

        @JvmField
        val ESCAPABLE: Pattern = Pattern.compile('^'.toString() + Escaping.ESCAPABLE)

        private fun calculateInlines(inlines: MutableList<InlineProcessor>): MutableMap<Char, MutableList<InlineProcessor>> {
            val map = HashMap<Char, MutableList<InlineProcessor>>(inlines.size)
            for (inlineProcessor in inlines) {
                val character = inlineProcessor.specialCharacter()
                val list = map.getOrPut(character) { ArrayList() }
                list.add(inlineProcessor)
            }
            return map
        }

        private fun calculateSpecialCharacters(inlineCharacters: MutableSet<Char>, delimiterCharacters: MutableSet<Char>): BitSet {
            val bitSet = BitSet()
            for (c in inlineCharacters) {
                bitSet.set(c.code)
            }
            for (c in delimiterCharacters) {
                bitSet.set(c.code)
            }
            return bitSet
        }

        private fun calculateDelimiterProcessors(delimiterProcessors: MutableList<DelimiterProcessor>): MutableMap<Char, DelimiterProcessor> {
            val map = HashMap<Char, DelimiterProcessor>()
            addDelimiterProcessors(delimiterProcessors, map)
            return map
        }

        private fun addDelimiterProcessors(
            delimiterProcessors: Iterable<DelimiterProcessor>,
            map: MutableMap<Char, DelimiterProcessor>
        ) {
            for (delimiterProcessor in delimiterProcessors) {
                val opening = delimiterProcessor.openingCharacter
                val closing = delimiterProcessor.closingCharacter
                if (opening == closing) {
                    val old = map[opening]
                    if (old != null && old.openingCharacter == old.closingCharacter) {
                        val s: StaggeredDelimiterProcessor = old as? StaggeredDelimiterProcessor
                            ?: StaggeredDelimiterProcessor(opening).apply { add(old) }
                        s.add(delimiterProcessor)
                        map[opening] = s
                    } else {
                        addDelimiterProcessorForChar(opening, delimiterProcessor, map)
                    }
                } else {
                    addDelimiterProcessorForChar(opening, delimiterProcessor, map)
                    addDelimiterProcessorForChar(closing, delimiterProcessor, map)
                }
            }
        }

        private fun addDelimiterProcessorForChar(
            delimiterChar: Char,
            toAdd: DelimiterProcessor,
            delimiterProcessors: MutableMap<Char, DelimiterProcessor>
        ) {
            val existing = delimiterProcessors.put(delimiterChar, toAdd)
            require(existing == null) { "Delimiter processor conflict with delimiter char '$delimiterChar'" }
        }
    }
}
