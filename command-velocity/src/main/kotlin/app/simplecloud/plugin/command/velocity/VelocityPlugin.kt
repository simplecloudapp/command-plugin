package app.simplecloud.plugin.command.velocity

import app.simplecloud.plugin.command.shared.command.CloudCommandHandler
import app.simplecloud.plugin.command.shared.CommandPlugin
import app.simplecloud.plugin.command.velocity.command.VelocityCloudSender
import com.google.inject.Inject
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent
import com.velocitypowered.api.plugin.Dependency
import com.velocitypowered.api.plugin.Plugin
import com.velocitypowered.api.plugin.annotation.DataDirectory
import com.velocitypowered.api.proxy.ProxyServer
import org.incendo.cloud.SenderMapper
import org.incendo.cloud.execution.ExecutionCoordinator
import org.incendo.cloud.velocity.VelocityCommandManager
import java.nio.file.Path

/**
 * @author Fynn Bauer in 2024
 */

@Plugin(
    id = "simplecloud-command",
    name = "simplecloud-command",
    version = BuildConstants.VERSION,
    authors = ["SimpleCloud Maintainers"],
    description = "Proxy commands for managing groups, servers, and players across the network",
    url = "https://github.com/simplecloudapp/command-plugin",
    dependencies = [
        Dependency(id = "simplecloud-api")
    ]
)
class VelocityPlugin @Inject constructor(
    private val server: ProxyServer,
    @DataDirectory val dataDirectory: Path,
) {

    private val plugin = CommandPlugin(dataDirectory)

    @Subscribe
    fun onProxyInitialization(event: ProxyInitializeEvent) {
        plugin.loadConfig()
        CloudCommandHandler(createCommandManager(), plugin).createCloudCommand()
    }

    private fun createCommandManager(): VelocityCommandManager<VelocityCloudSender> {
        return VelocityCommandManager(
            server.pluginManager.ensurePluginContainer(this),
            server,
            ExecutionCoordinator.simpleCoordinator(),
            SenderMapper.create(::VelocityCloudSender, VelocityCloudSender::getCommandSource)
        )
    }

}