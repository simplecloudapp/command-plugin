package app.simplecloud.plugin.command.shared.resolver

import app.simplecloud.api.persistentserver.PersistentServer
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver

object PersistentServerTagResolver {
    fun of(server: PersistentServer): TagResolver = TagResolver.resolver(
        Placeholder.unparsed("persistent_name", server.name),
        Placeholder.unparsed("persistent_id", server.persistentServerId),
        Placeholder.unparsed("persistent_type", server.type.name.lowercase()),
        Placeholder.unparsed("persistent_active", (server.isActive ?: false).toString()),
        Placeholder.unparsed("persistent_min_memory", (server.minMemory ?: 0).toString()),
        Placeholder.unparsed("persistent_max_memory", (server.maxMemory ?: 0).toString()),
        Placeholder.unparsed("persistent_max_players", (server.maxPlayers ?: 0).toString()),
        Placeholder.unparsed("persistent_serverhost", server.serverhostId ?: "N/A"),
    )
}
