package app.simplecloud.plugin.command.shared.command.commands

import app.simplecloud.plugin.command.shared.command.CloudSender
import app.simplecloud.plugin.command.shared.utilities.CommandPermissions
import app.simplecloud.plugin.command.shared.CommandPlugin
import org.incendo.cloud.CommandManager
import org.incendo.cloud.kotlin.coroutines.extension.suspendingHandler
import org.incendo.cloud.permission.Permission
import org.slf4j.LoggerFactory

class ReloadCommand<C : CloudSender>(
    private val plugin: CommandPlugin,
    private val manager: CommandManager<C>
) {

    private val logger = LoggerFactory.getLogger(ReloadCommand::class.java)

    fun register() {
        manager.command(
            manager.commandBuilder("cloud", "sc", "simplecloud")
                .literal("reload")
                .suspendingHandler(plugin.scope) { context ->
                    runCatching {
                        plugin.config.reload()
                    }.onSuccess {
                        context.sender().sendMessage(plugin.messageConfiguration.msg(plugin.messageConfiguration.command.reload.success))
                    }.onFailure {
                        logger.error("Failed to reload messages.yml", it)
                        context.sender().sendMessage(plugin.messageConfiguration.msg(plugin.messageConfiguration.command.reload.failed))
                    }
                }
                .permission(Permission.permission(CommandPermissions.RELOAD))
                .build()
        )
    }
}
