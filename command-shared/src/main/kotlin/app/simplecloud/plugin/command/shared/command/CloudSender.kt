package app.simplecloud.plugin.command.shared.command

import net.kyori.adventure.text.Component

/**
 * @author Fynn Bauer in 2024
 */
interface CloudSender {

    fun sendMessage(message: Component)

    fun hasPermission(permission: String): Boolean

}