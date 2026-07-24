package app.simplecloud.plugin.command.shared

import app.simplecloud.plugin.api.shared.config.ConfigurationFactory
import app.simplecloud.plugin.command.shared.config.MessageConfig
import java.nio.file.Path

/**
 * @author Fynn Bauer in 2024
 */
class CommandPlugin(
    dirPath: Path
) {

    val config = ConfigurationFactory(dirPath.resolve("messages.yml").toFile(), MessageConfig::class.java)
    val messageConfiguration: MessageConfig
        get() = config.get()

    fun loadConfig() {
        config.loadOrCreate(MessageConfig())
    }

}