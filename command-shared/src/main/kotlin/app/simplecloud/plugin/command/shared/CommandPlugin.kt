package app.simplecloud.plugin.command.shared

import app.simplecloud.plugin.api.shared.config.ConfigurationFactory
import app.simplecloud.plugin.command.shared.config.MessageConfig
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import java.nio.file.Path

/**
 * @author Fynn Bauer in 2024
 */
class CommandPlugin(path: Path) {

    private val logger = LoggerFactory.getLogger(CommandPlugin::class.java)

    val config = ConfigurationFactory(path.resolve("messages.yml").toFile(), MessageConfig::class.java)
    val messageConfiguration: MessageConfig get() = config.get()

    // coroutine scope used for commands
    val scope = CoroutineScope(
        CoroutineName("simplecloud-command") +
                SupervisorJob() +
                Dispatchers.IO +
                CoroutineExceptionHandler { context, exception ->
                    logger.error("An error occurred in coroutine ${context[CoroutineName]?.name}", exception)
                }
    )

    fun startup() {
        config.loadOrCreate(MessageConfig())
    }

    fun shutdown() {
        scope.cancel()
    }

}
