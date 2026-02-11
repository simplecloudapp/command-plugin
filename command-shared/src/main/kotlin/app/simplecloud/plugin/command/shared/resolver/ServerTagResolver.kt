package app.simplecloud.plugin.command.shared.resolver

import app.simplecloud.api.server.Server
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver

object ServerTagResolver {
    fun of(server: Server): TagResolver {
        val displayName = if (server.isFromGroup) {
            "${server.group?.name ?: server.serverGroupId}-${server.numericalId}"
        } else {
            server.persistentServer?.name ?: server.persistentServerId ?: "Unknown"
        }
        return TagResolver.resolver(
            Placeholder.unparsed("server_id", server.serverId),
            Placeholder.unparsed("server_display_name", displayName),
            Placeholder.unparsed("server_numerical_id", server.numericalId.toString()),
            Placeholder.unparsed("server_group", server.serverGroupId ?: ""),
            Placeholder.unparsed("server_state", server.state.name.lowercase()),
            Placeholder.unparsed("server_players", (server.playerCount ?: 0).toString()),
            Placeholder.unparsed("server_max_players", server.maxPlayers.toString()),
            Placeholder.unparsed("server_min_memory", server.minMemory.toString()),
            Placeholder.unparsed("server_max_memory", server.maxMemory.toString()),
        )
    }
}
