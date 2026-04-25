package app.simplecloud.plugin.command.shared

import app.simplecloud.api.CloudApi
import app.simplecloud.plugin.command.shared.commands.*
import org.incendo.cloud.CommandManager
import org.incendo.cloud.context.CommandContext
import org.incendo.cloud.permission.Permission
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder

class CloudCommandHandler<C : CloudSender>(
    private val commandManager: CommandManager<C>,
    private val commandPlugin: CommandPlugin
) {
    private val api = CloudApi.create()

    fun createCloudCommand() {
        commandManager.command(
            commandManager.commandBuilder("cloud")
                .handler { context: CommandContext<C> ->
                    val messages = commandPlugin.messageConfiguration
                    val entries = listOf(
                        "/cloud group list" to CommandPermission.GROUP_LIST,
                        "/cloud group info <group>" to CommandPermission.GROUP_INFO,
                        "/cloud group start <group> [count]" to CommandPermission.GROUP_START,
                        "/cloud group stop <group> [id]" to CommandPermission.GROUP_STOP,
                        "/cloud server list [group]" to CommandPermission.SERVER_LIST,
                        "/cloud server info <group> <id>" to CommandPermission.SERVER_INFO,
                        "/cloud server start <group>" to CommandPermission.SERVER_START,
                        "/cloud server stop <group> <id>" to CommandPermission.SERVER_STOP,
                        "/cloud player list <group|ps> <target>" to CommandPermission.PLAYER_LIST,
                        "/cloud player info <player>" to CommandPermission.PLAYER_INFO,
                        "/cloud player send <player> <group|ps> <target>" to CommandPermission.PLAYER_SEND,
                        "/cloud player sendall <group|ps> <target>" to CommandPermission.PLAYER_SEND,
                        "/cloud player sendfrom <source> <group|ps> <target>" to CommandPermission.PLAYER_SEND,
                        "/cloud player message <player> <message>" to CommandPermission.PLAYER_MESSAGE,
                        "/cloud reload" to CommandPermission.RELOAD
                    ).filter { (_, permission) -> context.sender().hasPermission(permission) }

                    if (entries.isEmpty()) {
                        context.sender().sendMessage(messages.msg(messages.command.help.empty))
                        return@handler
                    }

                    context.sender().sendMessage(messages.msg(messages.command.help.title))
                    entries.forEach { (command, _) ->
                        context.sender().sendMessage(
                            messages.msg(messages.command.help.entry, Placeholder.unparsed("command", command))
                        )
                    }
                }
                .permission(Permission.permission(CommandPermission.ROOT))
                .build()
        )
        GroupCommand(api, commandPlugin).register(commandManager)
        ServerCommand(api, commandPlugin).register(commandManager)
        PlayerCommand(api, commandPlugin).register(commandManager)
        ReloadCommand(commandPlugin).register(commandManager)
    }
}
