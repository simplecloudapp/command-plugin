package app.simplecloud.plugin.command.velocity

import app.simplecloud.plugin.command.shared.CloudCommandHandler
import com.google.common.eventbus.Subscribe
import com.google.inject.Inject
import com.velocitypowered.api.command.CommandSource
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent
import com.velocitypowered.api.plugin.Plugin
import com.velocitypowered.api.plugin.PluginContainer
import com.velocitypowered.api.proxy.ProxyServer
import org.incendo.cloud.SenderMapper
import org.incendo.cloud.execution.ExecutionCoordinator
import org.incendo.cloud.velocity.VelocityCommandManager

/**
 * @author Fynn Bauer in 2024
 */

@Plugin(id = "command-plugin", name = "SimpleCloud Command Plugin", version = "0.0.1-EXPERIMENTAL")
class VelocityPlugin @Inject constructor(
    private val server: ProxyServer,
    private val pluginContainer: PluginContainer
) {

    private lateinit var commandManager: VelocityCommandManager<CommandSource>

    @Subscribe
    fun onProxyInitialization(event: ProxyInitializeEvent) {
        val executionCoordinator = ExecutionCoordinator.simpleCoordinator<CommandSource>()

        commandManager = VelocityCommandManager(
            pluginContainer,
            server,
            executionCoordinator,
            SenderMapper.identity()
        )

        val cloudCommandHandler = CloudCommandHandler(commandManager)
        cloudCommandHandler.createCloudCommand()
    }
}