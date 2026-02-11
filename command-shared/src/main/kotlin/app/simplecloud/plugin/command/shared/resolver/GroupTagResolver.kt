package app.simplecloud.plugin.command.shared.resolver

import app.simplecloud.api.group.Group
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver

object GroupTagResolver {
    fun of(group: Group): TagResolver = TagResolver.resolver(
        Placeholder.unparsed("group_name", group.name),
        Placeholder.unparsed("group_id", group.serverGroupId),
        Placeholder.unparsed("group_type", group.type.name.lowercase()),
        Placeholder.unparsed("group_min_memory", (group.minMemory ?: 0).toString()),
        Placeholder.unparsed("group_max_memory", (group.maxMemory ?: 0).toString()),
        Placeholder.unparsed("group_max_players", (group.maxPlayers ?: 0).toString()),
    )
}
