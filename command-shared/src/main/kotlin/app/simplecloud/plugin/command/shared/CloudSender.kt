package app.simplecloud.plugin.command.shared

/**
 * @author Fynn Bauer in 2024
 */
interface CloudSender {

    // TODO: support adventure components
    fun sendMessage(message: String)
}