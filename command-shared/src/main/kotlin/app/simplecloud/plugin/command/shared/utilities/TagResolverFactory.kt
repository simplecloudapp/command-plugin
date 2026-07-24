package app.simplecloud.plugin.command.shared.utilities

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver

fun tags(vararg entries: Pair<String, Any?>): TagResolver {
    return TagResolver.resolver(
        entries.map { (key, value) ->
            Placeholder.unparsed(key, value?.toString() ?: "")
        }
    )
}
