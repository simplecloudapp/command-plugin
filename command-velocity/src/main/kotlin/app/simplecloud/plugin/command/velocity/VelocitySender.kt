package app.simplecloud.plugin.command.velocity

import app.simplecloud.plugin.command.shared.CloudSender
import com.velocitypowered.api.command.CommandSource
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage

/**
 * @author Fynn Bauer in 2024
 */
class VelocitySender(private val commandSource: CommandSource) : CloudSender {

    fun getCommandSource(): CommandSource {
        return commandSource
    }

    override fun sendMessage(message: Component) {
        commandSource.sendMessage(message)
    }
}