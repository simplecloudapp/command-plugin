package app.simplecloud.plugin.command.shared

import app.simplecloud.plugin.command.shared.config.MessageConfig
import app.simplecloud.plugin.command.shared.config.YamlConfig

/**
 * @author Fynn Bauer in 2024
 */
open class CommandPlugin(
    dirPath: String
) {

    val config = YamlConfig(dirPath);
    val messageConfiguration = config.load<MessageConfig>("messages")!!
}