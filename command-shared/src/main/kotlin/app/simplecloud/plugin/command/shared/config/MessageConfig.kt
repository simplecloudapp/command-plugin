package app.simplecloud.plugin.command.shared.config

import org.spongepowered.configurate.objectmapping.ConfigSerializable

/**
 * @author Fynn Bauer in 2024
 */

@ConfigSerializable
data class MessageConfig(
    val startingService: String = "<gray>Starting service from group <green>",
    val stoppingService: String = "<gray>Stopping service from group <red>",
)