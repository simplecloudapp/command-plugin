package app.simplecloud.plugin.command.shared.command.commands

import app.simplecloud.plugin.command.shared.command.CloudSender
import app.simplecloud.plugin.command.shared.utilities.CommandPermissions
import app.simplecloud.plugin.command.shared.CommandPlugin
import org.incendo.cloud.CommandManager
import org.incendo.cloud.kotlin.coroutines.extension.suspendingHandler
import org.incendo.cloud.permission.Permission

class ReloadCommand(
    private val plugin: CommandPlugin
) {
    fun <C : CloudSender> register(commandManager: CommandManager<C>) {
        commandManager.command(
            commandManager.commandBuilder("cloud", "sc", "simplecloud")
                .literal("reload")
                .suspendingHandler { context ->
                    runCatching {
                        plugin.config.reload()
                    }.onSuccess {
                        context.sender().sendMessage(plugin.messageConfiguration.msg(plugin.messageConfiguration.command.reload.success))
                    }.onFailure {
                        context.sender().sendMessage(plugin.messageConfiguration.msg(plugin.messageConfiguration.command.reload.failed))
                    }
                }
                .permission(Permission.permission(CommandPermissions.RELOAD))
                .build()
        )
    }
}
