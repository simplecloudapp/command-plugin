package app.simplecloud.plugin.command.velocity

import app.simplecloud.plugin.command.shared.CloudCommandHandler
import app.simplecloud.plugin.command.shared.CloudSender
import app.simplecloud.plugin.command.shared.CommandPlugin
import app.simplecloud.plugin.command.shared.config.MessageConfig
import app.simplecloud.plugin.command.shared.config.YamlConfig
import com.google.inject.Inject
import com.velocitypowered.api.command.CommandSource
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent
import com.velocitypowered.api.plugin.Plugin
import com.velocitypowered.api.plugin.PluginContainer
import com.velocitypowered.api.plugin.annotation.DataDirectory
import com.velocitypowered.api.proxy.ProxyServer
import org.incendo.cloud.SenderMapper
import org.incendo.cloud.execution.ExecutionCoordinator
import org.incendo.cloud.velocity.VelocityCommandManager
import java.nio.file.Path
import kotlin.io.path.pathString

/**
 * @author Fynn Bauer in 2024
 */

@Plugin(id = "command-plugin", name = "SimpleCloud Command Plugin", authors = ["Kaseax"], version = "0.0.1-EXPERIMENTAL")
class VelocityPlugin @Inject constructor(
    private val server: ProxyServer,
    @DataDirectory val dataDirectory: Path,
    private val pluginContainer: PluginContainer
): CommandPlugin(dataDirectory.pathString) {

    private lateinit var commandManager: VelocityCommandManager<CloudSender>

    @Subscribe
    fun onProxyInitialization(event: ProxyInitializeEvent) {
        config.save("messages", messageConfiguration)

        val executionCoordinator = ExecutionCoordinator.simpleCoordinator<CloudSender>()

        val senderMapper = SenderMapper.create<CommandSource, CloudSender>(
            { commandSender -> VelocitySender(commandSender) },
            { cloudSender -> (cloudSender as VelocitySender).getCommandSource() }
        )

        commandManager = VelocityCommandManager(
            pluginContainer,
            server,
            executionCoordinator,
            senderMapper
        )

        val cloudCommandHandler = CloudCommandHandler(commandManager, this)
        cloudCommandHandler.createCloudCommand()
    }
}