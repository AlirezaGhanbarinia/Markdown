package io.noties.markwon.app.samples

import io.noties.markwon.Markwon
import io.noties.markwon.app.sample.ui.MarkwonTextViewSample
import io.noties.markwon.inlineparser.MarkwonInlineParser.Companion.factoryBuilderNoDefaults
import io.noties.markwon.inlineparser.MarkwonInlineParserPlugin.Companion.create
import io.noties.markwon.sample.annotations.MarkwonArtifact
import io.noties.markwon.sample.annotations.MarkwonSampleInfo
import io.noties.markwon.sample.annotations.Tag

@MarkwonSampleInfo(
    id = "20200629170857",
    title = "Inline parsing without defaults",
    description = "Configure inline parser plugin to **not** have any **inline** parsing",
    artifacts = [MarkwonArtifact.INLINE_PARSER],
    tags = [Tag.parsing]
)
class InlinePluginNoDefaultsSample : MarkwonTextViewSample() {
    public override fun render() {
        val md = "" +
                "# Heading\n" +
                "`code` inlined and **bold** here"

        val markwon = Markwon.builder(context)
            .usePlugin(create(factoryBuilderNoDefaults()))
            //                .usePlugin(MarkwonInlineParserPlugin.create(MarkwonInlineParser.factoryBuilderNoDefaults(), factoryBuilder -> {
//                    // if anything, they can be included here
////                    factoryBuilder.includeDefaults()
//                }))
            .build()

        markwon.setMarkdown(textView, md)
    }
}
