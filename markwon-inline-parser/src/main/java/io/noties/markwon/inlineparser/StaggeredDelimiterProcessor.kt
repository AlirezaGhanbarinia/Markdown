package io.noties.markwon.inlineparser

import org.commonmark.parser.delimiter.DelimiterProcessor
import org.commonmark.parser.delimiter.DelimiterRun
import java.util.LinkedList

internal class StaggeredDelimiterProcessor(private val delim: Char) : DelimiterProcessor {
    private var minLength = 0
    private val processors = LinkedList<DelimiterProcessor>() // in reverse getMinLength order

    override fun getOpeningCharacter(): Char {
        return delim
    }

    override fun getClosingCharacter(): Char {
        return delim
    }

    override fun getMinLength(): Int {
        return minLength
    }

    fun add(dp: DelimiterProcessor) {
        val len = dp.minLength
        val it = processors.listIterator()
        var added = false
        while (it.hasNext()) {
            val p = it.next()
            val pLen = p.minLength
            if (len > pLen) {
                it.previous()
                it.add(dp)
                added = true
                break
            } else require(len != pLen) { "Cannot add two delimiter processors for char '$delim' and minimum length $len" }
        }
        if (!added) {
            processors.add(dp)
            this.minLength = len
        }
    }

    private fun findProcessor(len: Int): DelimiterProcessor {
        for (p in processors) {
            if (p.minLength <= len) {
                return p
            }
        }
        return processors.first()
    }

    override fun process(opener: DelimiterRun, closer: DelimiterRun): Int {
        return findProcessor(opener.length()).process(opener, closer)
    }
}
