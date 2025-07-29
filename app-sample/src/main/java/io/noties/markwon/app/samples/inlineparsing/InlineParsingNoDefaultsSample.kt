package io.noties.markwon.app.samples.inlineparsing

import io.noties.markwon.AbstractMarkwonPlugin
import io.noties.markwon.Markwon
import io.noties.markwon.MarkwonPlugin
import io.noties.markwon.app.sample.ui.MarkwonTextViewSample
import io.noties.markwon.inlineparser.BackticksInlineProcessor
import io.noties.markwon.inlineparser.MarkwonInlineParser.Companion.factoryBuilderNoDefaults
import io.noties.markwon.inlineparser.MarkwonInlineParserPlugin
import io.noties.markwon.inlineparser.MarkwonInlineParserPlugin.Companion.create
import io.noties.markwon.sample.annotations.MarkwonArtifact
import io.noties.markwon.sample.annotations.MarkwonSampleInfo
import io.noties.markwon.sample.annotations.Tag

@MarkwonSampleInfo(
    id = "20200630170823",
    title = "Inline parsing no defaults",
    description = "Parsing only inline code and disable all the rest",
    artifacts = [MarkwonArtifact.INLINE_PARSER],
    tags = [Tag.inline, Tag.parsing]
)
class InlineParsingNoDefaultsSample : MarkwonTextViewSample() {
    public override fun render() {
        // a plugin with NO defaults registered

        val md = "no [links](#) for **you** `code`!"

        val markwon = Markwon.builder(context) // pass `MarkwonInlineParser.factoryBuilderNoDefaults()` no disable all
            .usePlugin(create(factoryBuilderNoDefaults()))
            .usePlugin(object : AbstractMarkwonPlugin() {
                override fun configure(registry: MarkwonPlugin.Registry) {
                    registry.require(
                        MarkwonInlineParserPlugin::class.java,
                        MarkwonPlugin.Action { plugin: MarkwonInlineParserPlugin? ->
                            plugin!!.factoryBuilder().addInlineProcessor(BackticksInlineProcessor())
                        })
                }
            })
            .build()

        markwon.setMarkdown(textView, md)
    }
}
