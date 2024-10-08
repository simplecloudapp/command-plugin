package app.simplecloud.plugin.command.bungeecord

import app.simplecloud.plugin.command.shared.CloudSender
import net.md_5.bungee.api.CommandSender

/**
 * @author Fynn Bauer in 2024
 */
class BungeeCordSender(private val commandSender: CommandSender): CloudSender {

    override fun sendMessage(message: String) {
        commandSender.sendMessage(message)
    }
}