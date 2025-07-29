package io.noties.markwon.app.samples

import io.noties.markwon.AbstractMarkwonPlugin
import io.noties.markwon.Markwon
import io.noties.markwon.app.sample.ui.MarkwonTextViewSample
import io.noties.markwon.inlineparser.MarkwonInlineParser.Companion.factoryBuilderNoDefaults
import io.noties.markwon.inlineparser.MarkwonInlineParserPlugin.Companion.create
import io.noties.markwon.sample.annotations.MarkwonArtifact
import io.noties.markwon.sample.annotations.MarkwonSampleInfo
import io.noties.markwon.sample.annotations.Tag
import org.commonmark.node.Block
import org.commonmark.parser.Parser

@MarkwonSampleInfo(
    id = "20200629171212",
    title = "No parsing",
    description = "All commonmark parsing is disabled (both inlines and blocks)",
    artifacts = [MarkwonArtifact.CORE],
    tags = [Tag.parsing, Tag.rendering]
)
class NoParsingSample : MarkwonTextViewSample() {
    public override fun render() {
        val md = "" +
                "# Heading\n" +
                "[link](#) was _here_ and `then` and it was:\n" +
                "> a quote\n" +
                "```java\n" +
                "final int someJavaCode = 0;\n" +
                "```\n"

        val markwon = Markwon.builder(context) // disable inline parsing
            .usePlugin(create(factoryBuilderNoDefaults()))
            .usePlugin(object : AbstractMarkwonPlugin() {
                override fun configureParser(builder: Parser.Builder) {
                    builder.enabledBlockTypes(mutableSetOf())
                }
            })
            .build()

        markwon.setMarkdown(textView, md)
    }
}
