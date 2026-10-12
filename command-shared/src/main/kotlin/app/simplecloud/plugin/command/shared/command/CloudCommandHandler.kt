package app.simplecloud.plugin.command.shared.command

import app.simplecloud.plugin.command.shared.utilities.CommandPermissions
import app.simplecloud.plugin.command.shared.CommandPlugin
import app.simplecloud.plugin.command.shared.command.commands.GroupCommand
import app.simplecloud.plugin.command.shared.command.commands.PlayerCommand
import app.simplecloud.plugin.command.shared.command.commands.ReloadCommand
import app.simplecloud.plugin.command.shared.command.commands.ServerCommand
import app.simplecloud.plugin.command.shared.utilities.ApiUtils
import app.simplecloud.plugin.command.shared.utilities.tags
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import org.incendo.cloud.CommandManager
import org.incendo.cloud.exception.ArgumentParseException
import org.incendo.cloud.exception.CommandExecutionException
import org.incendo.cloud.exception.InvalidSyntaxException
import org.incendo.cloud.exception.NoPermissionException
import org.incendo.cloud.exception.parsing.NumberParseException
import org.incendo.cloud.permission.Permission

class CloudCommandHandler<C : CloudSender>(
    private val manager: CommandManager<C>,
    private val plugin: CommandPlugin
) {
    private val api = plugin.api

    fun register() {
        manager.command(
            manager.commandBuilder("cloud", "sc", "simplecloud")
                .handler { context ->
                    val sender = context.sender()
                    val messages = plugin.messageConfiguration
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
                        sender.sendMessage(messages.msg(messages.command.help.empty))
                        return@handler
                    }

                    sender.sendMessage(messages.msg(messages.command.help.title))
                    entries.forEach { (command, _) ->
                        sender.sendMessage(messages.msg(messages.command.help.entry, Placeholder.unparsed("command", command)))
                    }
                }
                .permission(Permission.permission(CommandPermissions.ROOT))
                .build()
        )
        GroupCommand(api, plugin, manager).register()
        ServerCommand(api, plugin, manager).register()
        PlayerCommand(api, plugin, manager).register()
        ReloadCommand(plugin, manager).register()
        registerExceptionHandlers()
    }

    private fun registerExceptionHandlers() {
        manager.exceptionController()
            .registerHandler(CommandExecutionException::class.java) { context ->
                val cause = context.exception().cause ?: context.exception()
                val messages = plugin.messageConfiguration
                context.context().sender().sendMessage(messages.msg(messages.command.error.failed, tags("reason" to ApiUtils.reason(cause))))
            }
            .registerHandler(InvalidSyntaxException::class.java) { context ->
                val messages = plugin.messageConfiguration
                context.context().sender().sendMessage(messages.msg(messages.command.usage.invalid, tags("command" to "/${context.exception().correctSyntax()}")))
            }
            .registerHandler(NoPermissionException::class.java) { context ->
                val messages = plugin.messageConfiguration
                context.context().sender().sendMessage(messages.msg(messages.command.permission.denied))
            }
            .registerHandler(ArgumentParseException::class.java) { context ->
                val messages = plugin.messageConfiguration
                val cause = context.exception().cause
                val message = if (cause is NumberParseException) {
                    // the parser also fails on valid numbers that are out of range
                    val number = cause.input().toBigDecimalOrNull()
                    val range = cause.range()
                    when {
                        number == null -> messages.msg(messages.command.usage.invalidNumber, tags("value" to cause.input()))
                        number < range.min().toString().toBigDecimal() -> messages.msg(messages.command.usage.numberTooSmall, tags("min" to range.min()))
                        number > range.max().toString().toBigDecimal() -> messages.msg(messages.command.usage.numberTooLarge, tags("max" to range.max()))
                        else -> messages.msg(messages.command.usage.invalidNumber, tags("value" to cause.input()))
                    }
                } else {
                    messages.msg(messages.command.error.failed, tags("reason" to (cause.message ?: "Invalid argument")))
                }
                context.context().sender().sendMessage(message)
            }
    }
}