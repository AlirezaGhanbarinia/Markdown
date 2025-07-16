package io.noties.markwon.inlineparser;

import org.commonmark.node.HtmlInline;
import org.commonmark.node.Node;

import java.util.regex.Pattern;

public class HtmlInlineProcessor extends InlineProcessor {

    private static final String TAGNAME = "[A-Za-z][A-Za-z0-9-]*";
    private static final String ATTRIBUTENAME = "[a-zA-Z_:][a-zA-Z0-9:._-]*";
    private static final String UNQUOTEDVALUE = "[^\"'=<>`\\x00-\\x20]+";
    private static final String SINGLEQUOTEDVALUE = "'[^']*'";
    private static final String DOUBLEQUOTEDVALUE = "\"[^\"]*\"";
    private static final String ATTRIBUTEVALUE = "(?:" + UNQUOTEDVALUE + "|" + SINGLEQUOTEDVALUE + "|" + DOUBLEQUOTEDVALUE + ")";
    private static final String ATTRIBUTEVALUESPEC = "\\s*=\\s*" + ATTRIBUTEVALUE;
    private static final String ATTRIBUTE = "\\s+" + ATTRIBUTENAME + "(?:" + ATTRIBUTEVALUESPEC + ")?";

    private static final String OPENTAG = "<" + TAGNAME + "(?:" + ATTRIBUTE + ")*\\s*/?>";
    private static final String CLOSETAG = "</" + TAGNAME + "\\s*>";

    private static final String HTMLCOMMENT = "<!---->|<!--(?:-?[^>-])(?:-?[^-])*-->";
    private static final String PROCESSINGINSTRUCTION = "[<][?].*?[?][>]";
    private static final String DECLARATION = "<![A-Z]+\\s+[^>]*>";
    private static final String CDATA = "<!\\[CDATA\\[[\\s\\S]*?\\]\\]>";

    private static final String HTMLTAG = "(?:" + OPENTAG + "|" + CLOSETAG + "|" + HTMLCOMMENT
            + "|" + PROCESSINGINSTRUCTION + "|" + DECLARATION + "|" + CDATA + ")";
    private static final Pattern HTML_TAG = Pattern.compile("^" + HTMLTAG, Pattern.CASE_INSENSITIVE);

    @Override
    public char specialCharacter() {
        return '<';
    }

    @Override
    protected Node parse() {
        String matched = match(HTML_TAG);
        if (matched != null) {
            HtmlInline node = new HtmlInline();
            node.setLiteral(matched);
            return node;
        } else {
            return null;
        }
    }
}