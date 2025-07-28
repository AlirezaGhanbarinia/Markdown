package io.noties.markwon.inlineparser

import org.commonmark.node.HtmlInline
import org.commonmark.node.Node
import java.util.regex.Pattern

class HtmlInlineProcessor : InlineProcessor() {
    override fun specialCharacter(): Char {
        return '<'
    }

    override fun parse(): Node? {
        val matched = match(HTML_TAG)
        if (matched != null) {
            val node = HtmlInline()
            node.literal = matched
            return node
        } else {
            return null
        }
    }

    companion object {
        private const val TAGNAME = "[A-Za-z][A-Za-z0-9-]*"
        private const val ATTRIBUTENAME = "[a-zA-Z_:][a-zA-Z0-9:._-]*"
        private const val UNQUOTEDVALUE = "[^\"'=<>`\\x00-\\x20]+"
        private const val SINGLEQUOTEDVALUE = "'[^']*'"
        private const val DOUBLEQUOTEDVALUE = "\"[^\"]*\""
        private const val ATTRIBUTEVALUE = "(?:$UNQUOTEDVALUE|$SINGLEQUOTEDVALUE|$DOUBLEQUOTEDVALUE)"
        private const val ATTRIBUTEVALUESPEC = "\\s*=\\s*$ATTRIBUTEVALUE"
        private const val ATTRIBUTE = "\\s+$ATTRIBUTENAME(?:$ATTRIBUTEVALUESPEC)?"

        private const val OPENTAG = "<$TAGNAME(?:$ATTRIBUTE)*\\s*/?>"
        private const val CLOSETAG = "</$TAGNAME\\s*>"

        private const val HTMLCOMMENT = "<!---->|<!--(?:-?[^>-])(?:-?[^-])*-->"
        private const val PROCESSINGINSTRUCTION = "[<][?].*?[?][>]"
        private const val DECLARATION = "<![A-Z]+\\s+[^>]*>"
        private const val CDATA = "<!\\[CDATA\\[[\\s\\S]*?\\]\\]>"

        private const val HTMLTAG = ("(?:" + OPENTAG + "|" + CLOSETAG + "|" + HTMLCOMMENT
                + "|" + PROCESSINGINSTRUCTION + "|" + DECLARATION + "|" + CDATA + ")")
        private val HTML_TAG: Pattern = Pattern.compile("^$HTMLTAG", Pattern.CASE_INSENSITIVE)
    }
}