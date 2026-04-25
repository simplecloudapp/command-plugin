package app.simplecloud.plugin.command.shared.commands

import app.simplecloud.plugin.command.shared.CloudSender
import app.simplecloud.plugin.command.shared.CommandPermission
import app.simplecloud.plugin.command.shared.CommandPlugin
import org.incendo.cloud.CommandManager
import org.incendo.cloud.permission.Permission

class ReloadCommand(
    private val plugin: CommandPlugin
) {
    fun <C : CloudSender> register(commandManager: CommandManager<C>) {
        commandManager.command(
            commandManager.commandBuilder("cloud")
                .literal("reload")
                .handler { context ->
                    val messages = plugin.messageConfiguration
                    runCatching {
                        plugin.messageConfiguration
                    }.onSuccess {
                        context.sender().sendMessage(messages.msg(messages.command.reload.success))
                    }.onFailure {
                        context.sender().sendMessage(messages.msg(messages.command.reload.failed))
                    }
                }
                .permission(Permission.permission(CommandPermission.RELOAD))
                .build()
        )
    }
}
