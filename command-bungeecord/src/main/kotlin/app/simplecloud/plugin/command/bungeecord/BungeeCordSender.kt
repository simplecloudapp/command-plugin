package app.simplecloud.plugin.command.bungeecord

import app.simplecloud.plugin.command.shared.CloudSender
import net.kyori.adventure.text.Component
import net.md_5.bungee.api.CommandSender

/**
 * @author Fynn Bauer in 2024
 */
class BungeeCordSender(private val commandSender: CommandSender, private val plugin: BungeeCordPlugin): CloudSender {

    fun getCommandSender(): CommandSender {
        return commandSender
    }

    override fun sendMessage(message: Component) {
        plugin.adventure().sender(commandSender).sendMessage(message)
    }
}