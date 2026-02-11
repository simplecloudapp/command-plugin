package app.simplecloud.plugin.command.shared

import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver

fun miniMessage(message: String, vararg tagResolver: TagResolver) =
    MiniMessage.miniMessage().deserialize(message, *tagResolver)