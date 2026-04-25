package app.simplecloud.plugin.api.shared.config

import app.simplecloud.plugin.api.shared.extension.miniMessage
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.tag.Tag
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver

abstract class AbstractMessageConfig {
    protected abstract val variables: Map<String, String>

    private val resolver: TagResolver by lazy {
        TagResolver.resolver(
            variables.map { (key, value) ->
                TagResolver.resolver(
                    key,
                    Tag.selfClosingInserting(miniMessage.deserialize(value))
                )
            }
        )
    }

    fun msg(message: String, vararg extra: TagResolver): Component {
        return miniMessage.deserialize(
            message,
            TagResolver.resolver(resolver, *extra)
        )
    }
}
