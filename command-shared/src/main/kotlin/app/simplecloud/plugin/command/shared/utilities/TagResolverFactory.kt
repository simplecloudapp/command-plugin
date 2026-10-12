package app.simplecloud.plugin.command.shared.utilities

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver

private val regex = Regex("""^[^<>:'"\\\s]{1,32}$""")

fun tags(vararg entries: Pair<String, Any?>): TagResolver {
    return TagResolver.resolver(
        entries.map { (key, value) ->
            val text = value?.toString() ?: ""
            if (key == "playername" && regex.matches(text)) Placeholder.parsed(key, text) else Placeholder.unparsed(key, text)
        }
    )
}
