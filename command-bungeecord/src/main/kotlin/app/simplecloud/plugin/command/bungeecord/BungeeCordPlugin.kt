package app.simplecloud.plugin.command.bungeecord

import app.simplecloud.plugin.command.bungeecord.command.BungeeCloudSender
import app.simplecloud.plugin.command.shared.CommandPlugin
import app.simplecloud.plugin.command.shared.command.CloudCommandHandler
import net.kyori.adventure.platform.bungeecord.BungeeAudiences
import net.md_5.bungee.api.plugin.Plugin
import org.incendo.cloud.SenderMapper
import org.incendo.cloud.bungee.BungeeCommandManager
import org.incendo.cloud.execution.ExecutionCoordinator

/**
 * @author Fynn Bauer in 2024
 */
class BungeeCordPlugin : Plugin() {

    private val plugin = CommandPlugin(dataFolder.toPath())
    private val adventure = BungeeAudiences.create(this)

    override fun onEnable() {
        plugin.loadConfig()
        CloudCommandHandler(createCommandManager(), plugin).createCloudCommand()
    }

    override fun onDisable() {
        adventure.close()
    }

    fun adventure(): BungeeAudiences {
        checkNotNull(this.adventure) { "Cannot retrieve audience provider while plugin is not enabled" }
        return this.adventure
    }

    private fun createCommandManager(): BungeeCommandManager<BungeeCloudSender> {
        return BungeeCommandManager(
            this,
            ExecutionCoordinator.simpleCoordinator(),
            SenderMapper.create({ BungeeCloudSender(it, this) }, BungeeCloudSender::getCommandSender)
        )
    }

}