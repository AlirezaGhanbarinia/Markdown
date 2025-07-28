package io.noties.markwon.inlineparser;

import static io.noties.markwon.inlineparser.InlineParserUtils.mergeChildTextNodes;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.commonmark.internal.Bracket;
import org.commonmark.internal.Delimiter;
import org.commonmark.internal.inline.AsteriskDelimiterProcessor;
import org.commonmark.internal.inline.UnderscoreDelimiterProcessor;
import org.commonmark.internal.util.Escaping;
import org.commonmark.internal.util.LinkScanner;
import org.commonmark.node.LinkReferenceDefinition;
import org.commonmark.node.Node;
import org.commonmark.node.Text;
import org.commonmark.parser.InlineParser;
import org.commonmark.parser.InlineParserContext;
import org.commonmark.parser.InlineParserFactory;
import org.commonmark.parser.SourceLines;
import org.commonmark.parser.beta.Position;
import org.commonmark.parser.beta.Scanner;
import org.commonmark.parser.delimiter.DelimiterProcessor;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @see #factoryBuilder()
 * @see #factoryBuilderNoDefaults()
 * @see FactoryBuilder
 * @since 4.2.0
 */
public class MarkwonInlineParser implements InlineParser, MarkwonInlineParserContext {

    @SuppressWarnings("unused")
    public interface FactoryBuilder {

        /**
         * @see InlineProcessor
         */
        @NonNull
        FactoryBuilder addInlineProcessor(@NonNull InlineProcessor processor);

        /**
         * @see AsteriskDelimiterProcessor
         * @see UnderscoreDelimiterProcessor
         */
        @NonNull
        FactoryBuilder addDelimiterProcessor(@NonNull DelimiterProcessor processor);

        /**
         * Indicate if markdown references are enabled. By default = `true`
         */
        @NonNull
        FactoryBuilder referencesEnabled(boolean referencesEnabled);

        @NonNull
        FactoryBuilder excludeInlineProcessor(@NonNull Class<? extends InlineProcessor> processor);

        @NonNull
        FactoryBuilder excludeDelimiterProcessor(@NonNull Class<? extends DelimiterProcessor> processor);

        @NonNull
        InlineParserFactory build();
    }

    public interface FactoryBuilderNoDefaults extends FactoryBuilder {
        /**
         * Includes all default delimiter and inline processors, and sets {@code referencesEnabled=true}.
         * Useful with subsequent calls to {@link #excludeInlineProcessor(Class)} or {@link #excludeDelimiterProcessor(Class)}
         */
        @NonNull
        FactoryBuilder includeDefaults();
    }

    /**
     * Creates an instance of {@link FactoryBuilder} and includes all defaults.
     *
     * @see #factoryBuilderNoDefaults()
     */
    @NonNull
    public static FactoryBuilder factoryBuilder() {
        return new FactoryBuilderImpl().includeDefaults();
    }

    /**
     * NB, this return an <em>empty</em> builder, so if no {@link FactoryBuilderNoDefaults#includeDefaults()}
     * is called, it means effectively <strong>no inline parsing</strong> (unless further calls
     * to {@link FactoryBuilder#addInlineProcessor(InlineProcessor)} or {@link FactoryBuilder#addDelimiterProcessor(DelimiterProcessor)}).
     */
    @NonNull
    public static FactoryBuilderNoDefaults factoryBuilderNoDefaults() {
        return new FactoryBuilderImpl();
    }

    private static final String ASCII_PUNCTUATION = "!\"#\\$%&'\\(\\)\\*\\+,\\-\\./:;<=>\\?@\\[\\\\\\]\\^_`\\{\\|\\}~";
    private static final Pattern PUNCTUATION = Pattern.compile("^[" + ASCII_PUNCTUATION + "\\p{Pc}\\p{Pd}\\p{Pe}\\p{Pf}\\p{Pi}\\p{Po}\\p{Ps}]");

    private static final Pattern SPNL = Pattern.compile("^ *(?:\n *)?");

    private static final Pattern UNICODE_WHITESPACE_CHAR = Pattern.compile("^[\\p{Zs}\t\r\n\f]");

    static final Pattern ESCAPABLE = Pattern.compile('^' + Escaping.ESCAPABLE);
    static final Pattern WHITESPACE = Pattern.compile("\\s+");

    private final InlineParserContext inlineParserContext;

    private final boolean referencesEnabled;

    private final BitSet specialCharacters;
    private final Map<Character, List<InlineProcessor>> inlineProcessors;
    private final Map<Character, DelimiterProcessor> delimiterProcessors;

    // currently we still hold a reference to it because we decided not to
    //  pass previous node argument to inline-processors (current usage is limited with NewLineInlineProcessor)
    private Node block;
    private SourceLines input;
    private Scanner scanner;

    /**
     * Top delimiter (emphasis, strong emphasis or custom emphasis). (Brackets are on a separate stack, different
     * from the algorithm described in the spec.)
     */
    private Delimiter lastDelimiter;

    /**
     * Top opening bracket (<code>[</code> or <code>![)</code>).
     */
    private Bracket lastBracket;

    // might we construct these in factory?
    public MarkwonInlineParser(
            @NonNull InlineParserContext inlineParserContext,
            boolean referencesEnabled,
            @NonNull List<InlineProcessor> inlineProcessors,
            @NonNull List<DelimiterProcessor> delimiterProcessors) {
        this.inlineParserContext = inlineParserContext;
        this.referencesEnabled = referencesEnabled;
        this.inlineProcessors = calculateInlines(inlineProcessors);
        this.delimiterProcessors = calculateDelimiterProcessors(delimiterProcessors);
        this.specialCharacters = calculateSpecialCharacters(
                this.inlineProcessors.keySet(),
                this.delimiterProcessors.keySet());
    }

    @NonNull
    private static Map<Character, List<InlineProcessor>> calculateInlines(@NonNull List<InlineProcessor> inlines) {
        final Map<Character, List<InlineProcessor>> map = new HashMap<>(inlines.size());
        List<InlineProcessor> list;
        for (InlineProcessor inlineProcessor : inlines) {
            final char character = inlineProcessor.specialCharacter();
            list = map.get(character);
            if (list == null) {
                list = new ArrayList<>(1);
                map.put(character, list);
            }
            list.add(inlineProcessor);
        }
        return map;
    }

    @NonNull
    private static BitSet calculateSpecialCharacters(Set<Character> inlineCharacters, Set<Character> delimiterCharacters) {
        final BitSet bitSet = new BitSet();
        for (Character c : inlineCharacters) {
            bitSet.set(c);
        }
        for (Character c : delimiterCharacters) {
            bitSet.set(c);
        }
        return bitSet;
    }

    private static Map<Character, DelimiterProcessor> calculateDelimiterProcessors(List<DelimiterProcessor> delimiterProcessors) {
        Map<Character, DelimiterProcessor> map = new HashMap<>();
        addDelimiterProcessors(delimiterProcessors, map);
        return map;
    }

    private static void addDelimiterProcessors(Iterable<DelimiterProcessor> delimiterProcessors, Map<Character, DelimiterProcessor> map) {
        for (DelimiterProcessor delimiterProcessor : delimiterProcessors) {
            char opening = delimiterProcessor.getOpeningCharacter();
            char closing = delimiterProcessor.getClosingCharacter();
            if (opening == closing) {
                DelimiterProcessor old = map.get(opening);
                if (old != null && old.getOpeningCharacter() == old.getClosingCharacter()) {
                    StaggeredDelimiterProcessor s;
                    if (old instanceof StaggeredDelimiterProcessor) {
                        s = (StaggeredDelimiterProcessor) old;
                    } else {
                        s = new StaggeredDelimiterProcessor(opening);
                        s.add(old);
                    }
                    s.add(delimiterProcessor);
                    map.put(opening, s);
                } else {
                    addDelimiterProcessorForChar(opening, delimiterProcessor, map);
                }
            } else {
                addDelimiterProcessorForChar(opening, delimiterProcessor, map);
                addDelimiterProcessorForChar(closing, delimiterProcessor, map);
            }
        }
    }

    private static void addDelimiterProcessorForChar(char delimiterChar, DelimiterProcessor toAdd, Map<Character, DelimiterProcessor> delimiterProcessors) {
        DelimiterProcessor existing = delimiterProcessors.put(delimiterChar, toAdd);
        if (existing != null) {
            throw new IllegalArgumentException("Delimiter processor conflict with delimiter char '" + delimiterChar + "'");
        }
    }

    /**
     * Parse content in block into inline children, using reference map to resolve references.
     */
    @Override
    public void parse(SourceLines content, Node block) {
        reset(content);

        // we still reference it
        this.block = block;

        while (true) {
            Node node = parseInline();
            if (node != null) {
                block.appendChild(node);
            } else {
                break;
            }
        }

        processDelimiters(null);
        mergeChildTextNodes(block);
    }

    private void reset(SourceLines content) {
        this.input = content;
        this.scanner = Scanner.of(content);
        this.lastDelimiter = null;
        this.lastBracket = null;
    }

    /**
     * Parse the next inline element in subject, advancing input index.
     * On success, add the result to block's children and return true.
     * On failure, return false.
     */
    @Nullable
    private Node parseInline() {

        final char c = peek();

        if (c == Scanner.END) {
            return null;
        }

        Node node = null;

        final List<InlineProcessor> inlines = this.inlineProcessors.get(c);

        if (inlines != null) {
            Position pos = scanner.position();

            for (InlineProcessor inline : inlines) {
                node = inline.parse(this);
                if (node != null) {
                    break;
                }

                // reset after each iteration (happens only when node is null)
                scanner.setPosition(pos);
            }
        } else {
            final DelimiterProcessor delimiterProcessor = delimiterProcessors.get(c);
            if (delimiterProcessor != null) {
                node = parseDelimiters(delimiterProcessor, c);
            } else {
                node = parseString();
            }
        }

        if (node != null) {
            return node;
        } else {
            scanner.next();
            // When we get here, it's only for a single special character that turned out to not have a special meaning.
            // So we shouldn't have a single surrogate here, hence it should be ok to turn it into a String.
            String literal = String.valueOf(c);
            return text(literal);
        }
    }

    /**
     * If RE matches at current index in the input, advance index and return the match; otherwise return null.
     */
    @Override
    @Nullable
    public String match(@NonNull Pattern re) {
        final Position start = scanner.position();

        if (!scanner.hasNext()) {
            return null;
        }

        StringBuilder remaining = new StringBuilder();
        while (scanner.hasNext()) {
            remaining.append(scanner.peek());
            scanner.next();
        }

        scanner.setPosition(start);

        Matcher matcher = re.matcher(remaining);
        if (matcher.lookingAt()) {
            String matched = matcher.group();

            for (int i = 0; i < matched.length(); i++) {
                scanner.next();
            }
            return matched;
        }
        return null;
    }

    @NonNull
    @Override
    public Text text(@NonNull String text) {
        return new Text(text);
    }

    @NonNull
    @Override
    public Text text(@NonNull String text, int beginIndex, int endIndex) {
        return new Text(text.substring(beginIndex, endIndex));
    }

    @Nullable
    @Override
    public LinkReferenceDefinition getLinkReferenceDefinition(String label) {
        return referencesEnabled ? inlineParserContext.getLinkReferenceDefinition(label) : null;
    }

    /**
     * Returns the char at the current input index, or {@code '\0'} in case there are no more characters.
     */
    @Override
    public char peek() {
        return scanner.peek();
    }

    @NonNull
    @Override
    public Node block() {
        return block;
    }

    @NonNull
    @Override
    public SourceLines input() {
        return input;
    }

    @Override
    @NotNull
    public Scanner scanner() {
        return scanner;
    }

    @Override
    public Bracket lastBracket() {
        return lastBracket;
    }

    @Override
    public Delimiter lastDelimiter() {
        return lastDelimiter;
    }

    @Override
    public void addBracket(Bracket bracket) {
        if (lastBracket != null) {
            lastBracket.bracketAfter = true;
        }
        lastBracket = bracket;
    }

    @Override
    public void removeLastBracket() {
        lastBracket = lastBracket.previous;
    }

    /**
     * Parse zero or more space characters, including at most one newline.
     */
    @Override
    public void spnl() {
        match(SPNL);
    }

    /**
     * Attempt to parse delimiters like emphasis, strong emphasis or custom delimiters.
     */
    @Nullable
    private Node parseDelimiters(DelimiterProcessor delimiterProcessor, char delimiterChar) {
        DelimiterData res = scanDelimiters(delimiterProcessor, delimiterChar);
        if (res == null) {
            return null;
        }

        int count = res.count;
        Position start = scanner.position();

        for (int i = 0; i < count; i++) {
            scanner.next();
        }
        Position end = scanner.position();
        String literal = scanner.getSource(start, end).getContent();
        Text node = new Text(literal);
        Delimiter delimiter = new Delimiter(Collections.singletonList(node), delimiterChar, res.canOpen, res.canClose, lastDelimiter);
        if (lastDelimiter != null) {
            lastDelimiter.next = delimiter;
        }
        lastDelimiter = delimiter;
        return node;
    }


    /**
     * Attempt to parse link destination, returning the string or null if no match.
     */
    @Override
    @Nullable
    public String parseLinkDestination() {
        Position start = scanner.position();
        if (!LinkScanner.scanLinkDestination(scanner)) {
            return null;
        }

        Position end = scanner.position();

        SourceLines sourceLines = scanner.getSource(start, end);
        String dest = sourceLines.getContent();

        if (dest.length() >= 2 && dest.charAt(0) == '<' && dest.charAt(dest.length() - 1) == '>') {
            dest = dest.substring(1, dest.length() - 1);
        }

        return Escaping.unescapeString(dest);
    }

    /**
     * Attempt to parse link title (sans quotes), returning the string or null if no match.
     */
    @Override
    @Nullable
    public String parseLinkTitle() {
        Position start = scanner.position();
        if (!LinkScanner.scanLinkTitle(scanner)) {
            return null;
        }

        Position end = scanner.position();

        SourceLines lines = scanner.getSource(start, end);
        String title = lines.getContent();

        // chop off ', " or parens
        if (title.length() >= 2) {
            title = title.substring(1, title.length() - 1);
        } else {
            return null;
        }
        return Escaping.unescapeString(title);
    }

    /**
     * Attempt to parse a link label, returning number of characters parsed.
     */
    @Override
    public int parseLinkLabel() {
        if (!scanner.hasNext() || scanner.peek() != '[') {
            return 0;
        }

        Position start = scanner.position();

        scanner.next();
        Position contentStart = scanner.position();

        if (!LinkScanner.scanLinkLabelContent(scanner)) {
            return 0;
        }

        if (!scanner.hasNext() || scanner.peek() != ']') {
            return 0;
        }

        Position contentEnd = scanner.position();

        // spec: A link label can have at most 999 characters inside the square brackets.
        SourceLines labelContentLines = scanner.getSource(contentStart, contentEnd);
        String labelContent = labelContentLines.getContent();
        if (labelContent.length() > 999) {
            return 0;
        }

        scanner.next();

        Position end = scanner.position();
        SourceLines wholeLabelLines = scanner.getSource(start, end);
        return wholeLabelLines.getContent().length();
    }

    /**
     * Parse a run of ordinary characters, or a single character with a special meaning in markdown, as a plain string.
     */
    private Node parseString() {
        StringBuilder sb = new StringBuilder();
        while (scanner.hasNext()) {
            char c = scanner.peek();
            if (specialCharacters.get(c)) {
                break;
            }
            sb.append(c);
            scanner.next();
        }
        if (sb.length() > 0) {
            return new Text(sb.toString());
        } else {
            return null;
        }
    }

    /**
     * Scan a sequence of characters with code delimiterChar, and return information about the number of delimiters
     * and whether they are positioned such that they can open and/or close emphasis or strong emphasis.
     *
     * @return information about delimiter run, or {@code null}
     */
    private DelimiterData scanDelimiters(DelimiterProcessor delimiterProcessor, char delimiterChar) {
        Position start = scanner.position();

        int delimiterCount = 0;
        while (peek() == delimiterChar) {
            delimiterCount++;
            scanner.next();
        }

        if (delimiterCount < delimiterProcessor.getMinLength()) {
            scanner.setPosition(start);
            return null;
        }

        String before;
        int codePointBefore = scanner.peekPreviousCodePoint();
        if (codePointBefore == Scanner.END) {
            before = "\n";
        } else {
            before = String.valueOf((char) codePointBefore);
        }

        char charAfter = scanner.hasNext() ? scanner.peek() : Scanner.END;
        String after = (charAfter == Scanner.END) ? "\n" : String.valueOf(charAfter);

        // We could be more lazy here, in most cases we don't need to do every match case.
        boolean beforeIsPunctuation = PUNCTUATION.matcher(before).matches();
        boolean beforeIsWhitespace = UNICODE_WHITESPACE_CHAR.matcher(before).matches();
        boolean afterIsPunctuation = PUNCTUATION.matcher(after).matches();
        boolean afterIsWhitespace = UNICODE_WHITESPACE_CHAR.matcher(after).matches();

        boolean leftFlanking = !afterIsWhitespace && (!afterIsPunctuation || beforeIsWhitespace || beforeIsPunctuation);
        boolean rightFlanking = !beforeIsWhitespace && (!beforeIsPunctuation || afterIsWhitespace || afterIsPunctuation);
        boolean canOpen;
        boolean canClose;
        if (delimiterChar == '_') {
            canOpen = leftFlanking && (!rightFlanking || beforeIsPunctuation);
            canClose = rightFlanking && (!leftFlanking || afterIsPunctuation);
        } else {
            canOpen = leftFlanking && delimiterChar == delimiterProcessor.getOpeningCharacter();
            canClose = rightFlanking && delimiterChar == delimiterProcessor.getClosingCharacter();
        }

        scanner.setPosition(start);
        return new DelimiterData(delimiterCount, canOpen, canClose);
    }

    @Override
    public void processDelimiters(Delimiter stackBottom) {

        Map<Character, Delimiter> openersBottom = new HashMap<>();

        // find first closer above stackBottom:
        Delimiter closer = lastDelimiter;
        while (closer != null && closer.previous != stackBottom) {
            closer = closer.previous;
        }
        // move forward, looking for closers, and handling each
        while (closer != null) {
            char delimiterChar = closer.delimiterChar;

            DelimiterProcessor delimiterProcessor = delimiterProcessors.get(delimiterChar);
            if (!closer.canClose() || delimiterProcessor == null) {
                closer = closer.next;
                continue;
            }

            char openingDelimiterChar = delimiterProcessor.getOpeningCharacter();

            // Found delimiter closer. Now look back for first matching opener.
            boolean openerFound = false;
            boolean potentialOpenerFound = false;
            Delimiter opener = closer.previous;
            while (opener != null && opener != stackBottom && opener != openersBottom.get(delimiterChar)) {
                if (opener.canOpen() && opener.delimiterChar == openingDelimiterChar) {
                    potentialOpenerFound = true;
                    int useDelims = delimiterProcessor.process(opener, closer);
                    if (useDelims > 0) {
                        openerFound = true;
                        break;
                    }
                }
                opener = opener.previous;
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
                    openersBottom.put(delimiterChar, closer.previous);
                    if (!closer.canOpen()) {
                        // We can remove a closer that can't be an opener,
                        // once we've seen there's no matching opener:
                        removeDelimiterKeepNode(closer);
                    }
                }
                closer = closer.next;
                continue;
            }

            int useDelims = delimiterProcessor.process(opener, closer);
            if (useDelims <= 0) {
                closer = closer.next;
                continue;
            }

            for (int i = 0; i < useDelims; i++) {
                Text t = opener.characters.remove(opener.characters.size() - 1);
                removeNodeIfEmpty(t);
            }
            for (int i = 0; i < useDelims; i++) {
                Text t = closer.characters.remove(0);
                removeNodeIfEmpty(t);
            }

            removeDelimitersBetween(opener, closer);

            if (opener.length() == 0) {
                removeDelimiterAndNode(opener);
            }

            if (closer.length() == 0) {
                Delimiter next = closer.next;
                removeDelimiterAndNode(closer);
                closer = next;
            } else {
                closer = closer.next;
            }
        }

        // remove all delimiters
        while (lastDelimiter != null && lastDelimiter != stackBottom) {
            removeDelimiterKeepNode(lastDelimiter);
        }
    }

    private void removeNodeIfEmpty(Text text) {
        if (text.getLiteral().isEmpty()) {
            text.unlink();
        }
    }

    private void removeDelimitersBetween(Delimiter opener, Delimiter closer) {
        Delimiter delimiter = closer.previous;
        while (delimiter != null && delimiter != opener) {
            Delimiter previousDelimiter = delimiter.previous;
            removeDelimiterKeepNode(delimiter);
            delimiter = previousDelimiter;
        }
    }

    /**
     * Remove the delimiter and the corresponding text node. For used delimiters, e.g. `*` in `*foo*`.
     */
    private void removeDelimiterAndNode(Delimiter delim) {
        for (Text text : delim.characters) {
            text.unlink();
        }
        removeDelimiter(delim);
    }

    /**
     * Remove the delimiter but keep the corresponding node as text. For unused delimiters such as `_` in `foo_bar`.
     */
    private void removeDelimiterKeepNode(Delimiter delim) {
        removeDelimiter(delim);
    }

    private void removeDelimiter(Delimiter delim) {
        if (delim.previous != null) {
            delim.previous.next = delim.next;
        }
        if (delim.next == null) {
            // top of stack
            lastDelimiter = delim.previous;
        } else {
            delim.next.previous = delim.previous;
        }
    }

    private static class DelimiterData {

        final int count;
        final boolean canClose;
        final boolean canOpen;

        DelimiterData(int count, boolean canOpen, boolean canClose) {
            this.count = count;
            this.canOpen = canOpen;
            this.canClose = canClose;
        }
    }

    static class FactoryBuilderImpl implements FactoryBuilder, FactoryBuilderNoDefaults {

        private final List<InlineProcessor> inlineProcessors = new ArrayList<>(3);
        private final List<DelimiterProcessor> delimiterProcessors = new ArrayList<>(3);
        private boolean referencesEnabled;

        @NonNull
        @Override
        public FactoryBuilder addInlineProcessor(@NonNull InlineProcessor processor) {
            this.inlineProcessors.add(processor);
            return this;
        }

        @NonNull
        @Override
        public FactoryBuilder addDelimiterProcessor(@NonNull DelimiterProcessor processor) {
            this.delimiterProcessors.add(processor);
            return this;
        }

        @NonNull
        @Override
        public FactoryBuilder referencesEnabled(boolean referencesEnabled) {
            this.referencesEnabled = referencesEnabled;
            return this;
        }

        @NonNull
        @Override
        public FactoryBuilder includeDefaults() {

            // by default enabled
            this.referencesEnabled = true;

            this.inlineProcessors.addAll(Arrays.asList(
                    new AutolinkInlineProcessor(),
                    new BackslashInlineProcessor(),
                    new BackticksInlineProcessor(),
                    new BangInlineProcessor(),
                    new CloseBracketInlineProcessor(),
                    new EntityInlineProcessor(),
                    new HtmlInlineProcessor(),
                    new NewLineInlineProcessor(),
                    new OpenBracketInlineProcessor()));

            this.delimiterProcessors.addAll(Arrays.asList(
                    new AsteriskDelimiterProcessor(),
                    new UnderscoreDelimiterProcessor()));

            return this;
        }

        @NonNull
        @Override
        public FactoryBuilder excludeInlineProcessor(@NonNull Class<? extends InlineProcessor> type) {
            for (int i = 0, size = inlineProcessors.size(); i < size; i++) {
                if (type.equals(inlineProcessors.get(i).getClass())) {
                    inlineProcessors.remove(i);
                    break;
                }
            }
            return this;
        }

        @NonNull
        @Override
        public FactoryBuilder excludeDelimiterProcessor(@NonNull Class<? extends DelimiterProcessor> type) {
            for (int i = 0, size = delimiterProcessors.size(); i < size; i++) {
                if (type.equals(delimiterProcessors.get(i).getClass())) {
                    delimiterProcessors.remove(i);
                    break;
                }
            }
            return this;
        }

        @NonNull
        @Override
        public InlineParserFactory build() {
            return new InlineParserFactoryImpl(referencesEnabled, inlineProcessors, delimiterProcessors);
        }
    }

    static class InlineParserFactoryImpl implements InlineParserFactory {

        private final boolean referencesEnabled;
        private final List<InlineProcessor> inlineProcessors;
        private final List<DelimiterProcessor> delimiterProcessors;

        InlineParserFactoryImpl(
                boolean referencesEnabled,
                @NonNull List<InlineProcessor> inlineProcessors,
                @NonNull List<DelimiterProcessor> delimiterProcessors) {
            this.referencesEnabled = referencesEnabled;
            this.inlineProcessors = inlineProcessors;
            this.delimiterProcessors = delimiterProcessors;
        }

        @Override
        public InlineParser create(InlineParserContext inlineParserContext) {
            final List<DelimiterProcessor> delimiterProcessors;
            final List<DelimiterProcessor> customDelimiterProcessors = inlineParserContext.getCustomDelimiterProcessors();
            final int size = customDelimiterProcessors != null
                    ? customDelimiterProcessors.size()
                    : 0;
            if (size > 0) {
                delimiterProcessors = new ArrayList<>(size + this.delimiterProcessors.size());
                delimiterProcessors.addAll(this.delimiterProcessors);
                delimiterProcessors.addAll(customDelimiterProcessors);
            } else {
                delimiterProcessors = this.delimiterProcessors;
            }
            return new MarkwonInlineParser(
                    inlineParserContext,
                    referencesEnabled,
                    inlineProcessors,
                    delimiterProcessors);
        }
    }
}
