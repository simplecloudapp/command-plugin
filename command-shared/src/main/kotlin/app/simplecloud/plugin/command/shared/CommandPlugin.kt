package app.simplecloud.plugin.command.shared

import app.simplecloud.plugin.api.shared.config.ConfigurationFactory
import app.simplecloud.plugin.command.shared.config.MessageConfig
import java.nio.file.Path

/**
 * @author Fynn Bauer in 2024
 */
class CommandPlugin(path: Path) {

    val config = ConfigurationFactory(path.resolve("messages.yml").toFile(), MessageConfig::class.java)
    val messageConfiguration: MessageConfig get() = config.get()

    fun startup() {
        config.loadOrCreate(MessageConfig())
    }

}