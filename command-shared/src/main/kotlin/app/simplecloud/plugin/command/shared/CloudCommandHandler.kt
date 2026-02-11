package app.simplecloud.plugin.command.shared

import app.simplecloud.api.CloudApi
import app.simplecloud.plugin.command.shared.commands.*
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
            commandManager.commandBuilder("cloud")
                .handler { context: CommandContext<C> ->
                    context.sender().sendMessage(miniMessage(commandPlugin.messageConfiguration.cloudHelpMessage))
                }
                .permission(Permission.permission("simplecloud.command.cloud"))
                .build()
        )
        StopCommand(api, commandPlugin).register(commandManager)
        StartCommand(api, commandPlugin).register(commandManager)
        InfoCommand(api, commandPlugin).register(commandManager)
        EditCommand(api, commandPlugin).register(commandManager)
        DeleteCommand(api, commandPlugin).register(commandManager)
    }
}