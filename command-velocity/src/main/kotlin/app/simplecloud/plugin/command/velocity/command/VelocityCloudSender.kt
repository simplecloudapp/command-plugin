package app.simplecloud.plugin.command.velocity.command

import app.simplecloud.plugin.command.shared.command.CloudSender
import com.velocitypowered.api.command.CommandSource
import net.kyori.adventure.text.Component

/**
 * @author Fynn Bauer in 2024
 */
class VelocityCloudSender(private val commandSource: CommandSource) : CloudSender {

    fun getCommandSource(): CommandSource {
        return commandSource
    }

    override fun sendMessage(message: Component) {
        commandSource.sendMessage(message)
    }

    override fun hasPermission(permission: String): Boolean {
        return commandSource.hasPermission(permission)
    }
}