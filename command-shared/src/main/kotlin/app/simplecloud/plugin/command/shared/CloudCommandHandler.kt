package app.simplecloud.plugin.command.shared

import app.simplecloud.controller.api.ControllerApi
import org.incendo.cloud.CommandManager
import org.incendo.cloud.context.CommandContext
import org.incendo.cloud.parser.standard.LongParser.longParser
import org.incendo.cloud.parser.standard.StringParser.stringParser

class CloudCommandHandler<C : Any>(
    private val commandManager: CommandManager<C>
) {

    private val controllerApi = ControllerApi.create()

    fun createCloudCommand() {
        commandManager.command(
            commandManager.commandBuilder("cloud")
                .handler { context: CommandContext<C> -> println("Cloud command executed") }
                .build()
        )

        registerStartCommand()
        registerStopCommand()
        registerGetServersCommand()
        registerGetGroupsCommand()
    }

    private fun registerStartCommand() {
        commandManager.command(
            commandManager.commandBuilder("cloud")
                .literal("start")
                .required("group", stringParser())
                .handler { context: CommandContext<C> ->
                    val group = context.get<String>("group")
                    println("Starting service from group $group")
                    controllerApi.getServers().startServer(group)
                }
                .build()
        )
    }

    private fun registerStopCommand() {
        commandManager.command(
            commandManager.commandBuilder("cloud")
                .literal("stop")
                .required("group", stringParser())
                .required("id", longParser())
                .handler { context: CommandContext<C> ->
                    val group = context.get<String>("group")
                    val id = context.get<Long>("id")
                    println("Stopping service with ID $id from group $group")
                    controllerApi.getServers().stopServer(group, id)
                }
                .build()
        )
    }

    private fun registerGetServersCommand() {
        commandManager.command(
            commandManager.commandBuilder("cloud")
                .literal("get")
                .literal("servers", "server")
                .optional("group", stringParser())
                .optional("id", longParser())
                .handler { context: CommandContext<C> ->
                    val group = context.get<String>("group")
                    val id = context.get<Long>("id")

                    when {
                        group != null && id != null -> {
                            println("Getting server with ID $id from group $group")
                            controllerApi.getServers().getServerByNumerical(group, id).thenAccept { server ->
                                println("Group: ${server.group}")
                            }
                        }
                        group != null -> {
                            println("Getting servers from group $group")
                            controllerApi.getServers().getServersByGroup(group).thenAccept { servers ->
                                servers.forEach { server ->
                                    println("Group: ${server.group}")
                                }
                            }
                        }
                        id != null -> {
                            println("Getting server with ID $id")
                        }
                        else -> {
                            println("Getting all servers.")
                            controllerApi.getServers().getAllServers().thenAccept(::println)
                        }
                    }
                }
                .build()
        )
    }

    private fun registerGetGroupsCommand() {
        commandManager.command(
            commandManager.commandBuilder("cloud")
                .literal("get")
                .literal("groups", "group")
                .optional("group", stringParser())
                .handler { context: CommandContext<C> ->
                    val groupName = context.get<String>("group")
                    if (groupName != null) {
                        println("Getting group $groupName")
                        controllerApi.getGroups().getGroupByName(groupName).thenAccept { group ->
                            println("Group: ${group.name}")
                        }
                    } else {
                        controllerApi.getGroups().getAllGroups().thenAccept { groups ->
                            groups.forEach { group ->
                                println("Group: ${group.name}")
                            }
                        }
                    }
                }
                .build()
        )
    }
}
