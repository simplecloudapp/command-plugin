package app.simplecloud.plugin.command.shared.command

import app.simplecloud.api.CloudApi
import app.simplecloud.plugin.command.shared.utilities.CommandPermissions
import app.simplecloud.plugin.command.shared.CommandPlugin
import app.simplecloud.plugin.command.shared.command.commands.GroupCommand
import app.simplecloud.plugin.command.shared.command.commands.PlayerCommand
import app.simplecloud.plugin.command.shared.command.commands.ReloadCommand
import app.simplecloud.plugin.command.shared.command.commands.ServerCommand
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import org.incendo.cloud.CommandManager
import org.incendo.cloud.context.CommandContext
import org.incendo.cloud.permission.Permission

class CloudCommandHandler<C : CloudSender>(
    private val commandManager: CommandManager<C>,
    private val commandPlugin: CommandPlugin
) {
    private val api = CloudApi.create()

    fun createCloudCommand() {
        commandManager.command(
            commandManager.commandBuilder("cloud", "sc", "simplecloud")
                .handler { context: CommandContext<C> ->
                    val messages = commandPlugin.messageConfiguration
                    val entries = listOf(
                        "/cloud group list" to CommandPermissions.GROUP_LIST,
                        "/cloud group info <group>" to CommandPermissions.GROUP_INFO,
                        "/cloud group start <group> [count]" to CommandPermissions.GROUP_START,
                        "/cloud group stop <group> [id]" to CommandPermissions.GROUP_STOP,
                        "/cloud server list [group]" to CommandPermissions.SERVER_LIST,
                        "/cloud server info <group> <id>" to CommandPermissions.SERVER_INFO,
                        "/cloud server start <group>" to CommandPermissions.SERVER_START,
                        "/cloud server stop <group> <id>" to CommandPermissions.SERVER_STOP,
                        "/cloud player list <group|ps> <target>" to CommandPermissions.PLAYER_LIST,
                        "/cloud player info <player>" to CommandPermissions.PLAYER_INFO,
                        "/cloud player send <player> <group|ps> <target>" to CommandPermissions.PLAYER_SEND,
                        "/cloud player sendall <group|ps> <target>" to CommandPermissions.PLAYER_SEND,
                        "/cloud player sendfrom <source> <group|ps> <target>" to CommandPermissions.PLAYER_SEND,
                        "/cloud player message <player> <message>" to CommandPermissions.PLAYER_MESSAGE,
                        "/cloud reload" to CommandPermissions.RELOAD
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
                .permission(Permission.permission(CommandPermissions.ROOT))
                .build()
        )
        GroupCommand(api, commandPlugin).register(commandManager)
        ServerCommand(api, commandPlugin).register(commandManager)
        PlayerCommand(api, commandPlugin).register(commandManager)
        ReloadCommand(commandPlugin).register(commandManager)
    }
}