package app.simplecloud.plugin.command.shared

import net.kyori.adventure.text.Component

/**
 * @author Fynn Bauer in 2024
 */
interface CloudSender {

    fun sendMessage(message: Component)
}