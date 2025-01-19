package app.simplecloud.plugin.command.bungeecord

import app.simplecloud.plugin.command.shared.CloudCommandHandler
import app.simplecloud.plugin.command.shared.CloudSender
import app.simplecloud.plugin.command.shared.CommandPlugin
import net.kyori.adventure.platform.bungeecord.BungeeAudiences
import net.md_5.bungee.api.CommandSender
import net.md_5.bungee.api.plugin.Plugin
import org.incendo.cloud.SenderMapper
import org.incendo.cloud.bungee.BungeeCommandManager
import org.incendo.cloud.execution.ExecutionCoordinator


/**
 * @author Fynn Bauer in 2024
 */
class BungeeCordPlugin(): Plugin() {

    private lateinit var commandManager: BungeeCommandManager<CloudSender>
    private lateinit var commandPlugin: CommandPlugin

    private val adventure = BungeeAudiences.create(this)

    override fun onEnable() {
        commandPlugin = CommandPlugin(this.dataFolder.path)

        commandPlugin.config.save("messages", commandPlugin.messageConfiguration)

        val executionCoordinator = ExecutionCoordinator.simpleCoordinator<CloudSender>()

        val senderMapper = SenderMapper.create<CommandSender, CloudSender>(
            { commandSender -> BungeeCordSender(commandSender, this) },
            { cloudSender -> (cloudSender as BungeeCordSender).getCommandSender() }
        )

        commandManager = BungeeCommandManager(
            this,
            executionCoordinator,
            senderMapper
        )

        val cloudCommandHandler = CloudCommandHandler(commandManager, commandPlugin)
        cloudCommandHandler.createCloudCommand()
    }

    override fun onDisable() {
        adventure.close()
    }

    fun adventure(): BungeeAudiences {
        checkNotNull(this.adventure) { "Cannot retrieve audience provider while plugin is not enabled" }
        return this.adventure
    }

}