package io.noties.markwon.inlineparser

import io.noties.markwon.AbstractMarkwonPlugin
import io.noties.markwon.inlineparser.MarkwonInlineParser.Companion.factoryBuilder
import io.noties.markwon.inlineparser.MarkwonInlineParser.FactoryBuilder
import org.commonmark.parser.Parser

/**
 * @since 4.3.0
 */
class MarkwonInlineParserPlugin internal constructor(
    private val factoryBuilder: FactoryBuilder
) : AbstractMarkwonPlugin() {
    interface BuilderConfigure<B : FactoryBuilder> {
        fun configureBuilder(factoryBuilder: B)
    }

    override fun configureParser(builder: Parser.Builder) {
        builder.inlineParserFactory(factoryBuilder.build())
    }

    fun factoryBuilder(): FactoryBuilder {
        return factoryBuilder
    }

    companion object {

        @JvmStatic
        @JvmOverloads
        fun create(configure: BuilderConfigure<FactoryBuilder>): MarkwonInlineParserPlugin {
            val factoryBuilder = factoryBuilder()
            configure.configureBuilder(factoryBuilder)
            return MarkwonInlineParserPlugin(factoryBuilder)
        }

        @JvmStatic
        @JvmOverloads
        fun create(factoryBuilder: FactoryBuilder = factoryBuilder()): MarkwonInlineParserPlugin {
            return MarkwonInlineParserPlugin(factoryBuilder)
        }

        fun <B : FactoryBuilder> create(factoryBuilder: B, configure: BuilderConfigure<B>): MarkwonInlineParserPlugin {
            configure.configureBuilder(factoryBuilder)
            return MarkwonInlineParserPlugin(factoryBuilder)
        }
    }
}
