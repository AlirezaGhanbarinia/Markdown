package io.noties.markwon.inlineparser

import org.commonmark.internal.util.Escaping
import org.commonmark.internal.util.Html5Entities
import org.commonmark.node.Node
import java.util.regex.Pattern

/**
 * Parses HTML entities `&amp;`
 *
 * @since 4.2.0
 */
class EntityInlineProcessor : InlineProcessor() {
    override fun specialCharacter(): Char {
        return '&'
    }

    override fun parse(): Node? {
        val m: String?
        return if ((match(ENTITY_HERE).also { m = it }) != null) {
            text(Html5Entities.entityToString(m))
        } else {
            null
        }
    }

    companion object {
        private val ENTITY_HERE: Pattern = Pattern.compile('^'.toString() + Escaping.ENTITY, Pattern.CASE_INSENSITIVE)
    }
}
