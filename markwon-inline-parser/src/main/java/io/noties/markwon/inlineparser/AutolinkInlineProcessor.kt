package io.noties.markwon.inlineparser

import org.commonmark.node.Link
import org.commonmark.node.Node
import org.commonmark.node.Text
import java.util.regex.Pattern

/**
 * Parses autolinks, for example `<me@mydoma.in>`
 *
 * @since 4.2.0
 */
class AutolinkInlineProcessor : InlineProcessor() {
    override fun specialCharacter(): Char {
        return '<'
    }

    override fun parse(): Node? {
        var m: String?
        if ((match(EMAIL_AUTOLINK).also { m = it }) != null) {
            val dest = m!!.substring(1, m.length - 1)
            val node = Link("mailto:$dest", null)
            node.appendChild(Text(dest))
            return node
        } else if ((match(AUTOLINK).also { m = it }) != null) {
            val dest = m!!.substring(1, m.length - 1)
            val node = Link(dest, null)
            node.appendChild(Text(dest))
            return node
        } else {
            return null
        }
    }

    companion object {
        private val EMAIL_AUTOLINK: Pattern = Pattern
            .compile("^<([a-zA-Z0-9.!#$%&'*+/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*)>")

        private val AUTOLINK: Pattern = Pattern
            .compile("^<[a-zA-Z][a-zA-Z0-9.+-]{1,31}:[^<>\u0000-\u0020]*>")
    }
}
