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
                    // We don't initialize a variable here to use the new config values in the message configuration
                    runCatching {
                        // TODO: reload
                        //plugin.config.reload()
                    }.onSuccess {
                        context.sender().sendMessage(plugin.messageConfiguration.msg(plugin.messageConfiguration.command.reload.success))
                    }.onFailure {

                        context.sender().sendMessage(plugin.messageConfiguration.msg(plugin.messageConfiguration.command.reload.failed))
                    }
                }
                .permission(Permission.permission(CommandPermission.RELOAD))
                .build()
        )
    }
}
