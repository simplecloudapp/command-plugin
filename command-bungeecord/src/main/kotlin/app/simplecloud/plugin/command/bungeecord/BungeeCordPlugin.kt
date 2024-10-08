package app.simplecloud.plugin.command.bungeecord

import app.simplecloud.plugin.command.shared.CloudCommandHandler
import app.simplecloud.plugin.command.shared.CloudSender
import net.md_5.bungee.api.CommandSender
import net.md_5.bungee.api.connection.ProxiedPlayer
import net.md_5.bungee.api.plugin.Plugin
import org.incendo.cloud.SenderMapper
import org.incendo.cloud.bungee.BungeeCommandManager
import org.incendo.cloud.execution.ExecutionCoordinator

/**
 * @author Fynn Bauer in 2024
 */
class BungeeCordPlugin(): Plugin() {

    private lateinit var commandManager: BungeeCommandManager<CloudSender>

    override fun onEnable() {
        val executionCoordinator = ExecutionCoordinator.simpleCoordinator<CloudSender>()

        val senderMapper = SenderMapper.create<CommandSender, CloudSender>(
            { commandSender -> BungeeCordSender(commandSender) },
            { cloudSender -> (cloudSender as BungeeCordSender).getCommandSender() }
        )

        commandManager = BungeeCommandManager(
            this,
            executionCoordinator,
            senderMapper
        )

        val cloudCommandHandler = CloudCommandHandler(commandManager)
        cloudCommandHandler.createCloudCommand()
    }

}