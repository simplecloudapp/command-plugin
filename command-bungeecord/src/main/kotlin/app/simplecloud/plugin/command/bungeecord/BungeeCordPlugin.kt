package app.simplecloud.plugin.command.bungeecord

import app.simplecloud.plugin.command.shared.CloudCommandHandler
import net.md_5.bungee.api.CommandSender
import net.md_5.bungee.api.plugin.Plugin
import org.incendo.cloud.SenderMapper
import org.incendo.cloud.bungee.BungeeCommandManager
import org.incendo.cloud.execution.ExecutionCoordinator

/**
 * @author Fynn Bauer in 2024
 */
class BungeeCordPlugin(): Plugin() {

    private lateinit var commandManager: BungeeCommandManager<CommandSender>

    override fun onEnable() {
        val exeuctionCoordinator = ExecutionCoordinator.simpleCoordinator<CommandSender>()

        commandManager = BungeeCommandManager(
            this,
            exeuctionCoordinator,
            SenderMapper.identity()
        )

        val cloudCommandHandler = CloudCommandHandler(commandManager)
        cloudCommandHandler.createCloudCommand()
    }

}