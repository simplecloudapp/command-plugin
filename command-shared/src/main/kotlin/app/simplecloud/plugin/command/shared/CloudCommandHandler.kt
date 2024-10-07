package app.simplecloud.plugin.command.shared

import org.incendo.cloud.Command
import org.incendo.cloud.CommandManager
import org.incendo.cloud.component.CommandComponent
import org.incendo.cloud.context.CommandContext
import org.incendo.cloud.meta.CommandMeta
import org.incendo.cloud.parser.standard.StringParser.stringParser

/**
 * @author Fynn Bauer in 2024
 */

/**
 * Generic command handler for cloud commands
 * @param C the command sender type used to execute commands
 */
class CloudCommandHandler<C : Any>(
    private val commandManager: CommandManager<C>
) {

    fun createCloudCommand(): CommandManager<C> {
        return commandManager.command(
            commandManager.commandBuilder("cloud")
                .literal("start")
                .required("name", stringParser())
                .handler { context: CommandContext<C> ->
                    val name = context.get<String>("name")
                    println("Starting service with name $name")
                }.build()
        )
    }
}