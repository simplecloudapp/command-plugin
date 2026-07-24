package app.simplecloud.plugin.command.bungeecord.command

import app.simplecloud.plugin.command.bungeecord.BungeeCordPlugin
import app.simplecloud.plugin.command.shared.command.CloudSender
import net.kyori.adventure.text.Component
import net.md_5.bungee.api.CommandSender

/**
 * @author Fynn Bauer in 2024
 */
class BungeeCloudSender(private val commandSender: CommandSender, private val plugin: BungeeCordPlugin): CloudSender {

    fun getCommandSender(): CommandSender {
        return commandSender
    }

    override fun sendMessage(message: Component) {
        plugin.adventure().sender(commandSender).sendMessage(message)
    }

    override fun hasPermission(permission: String): Boolean {
        return commandSender.hasPermission(permission)
    }
}