package app.simplecloud.plugin.command.shared

import app.simplecloud.controller.api.ControllerApi
import org.incendo.cloud.CommandManager
import org.incendo.cloud.context.CommandContext
import org.incendo.cloud.parser.standard.LongParser.longParser
import org.incendo.cloud.parser.standard.StringParser.stringParser
import org.incendo.cloud.suggestion.Suggestion
import org.incendo.cloud.suggestion.SuggestionProvider

class CloudCommandHandler<C : CloudSender>(
    private val commandManager: CommandManager<C>
) {

    private val controllerApi = ControllerApi.createFutureApi()

    fun createCloudCommand() {
        commandManager.command(
            commandManager.commandBuilder("cloud")
                .handler { _: CommandContext<C> -> println("Cloud command executed") }
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
                .required(
                    "group",
                    stringParser(),
                    SuggestionProvider { _, _ ->
                        controllerApi.getGroups().getAllGroups().thenApply { groups ->
                            groups.map { group -> Suggestion.suggestion(group.name) }
                        }
                    }
                )
                .handler { context: CommandContext<C> ->
                    val group = context.get<String>("group")
                    context.sender().sendMessage("Starting service from group $group")
                    controllerApi.getServers().startServer(group)
                }
                .build()
        )
    }

    private fun registerStopCommand() {
        commandManager.command(
            commandManager.commandBuilder("cloud")
                .literal("stop")
                .required("group", stringParser(), SuggestionProvider { _, _ ->
                    controllerApi.getGroups().getAllGroups().thenApply { groups ->
                        groups.map { group -> Suggestion.suggestion(group.name) }
                    }
                })
                .required("id", longParser(), SuggestionProvider { _, _ ->
                    controllerApi.getServers().getAllServers().thenApply { servers ->
                        servers.map { server -> Suggestion.suggestion(server.numericalId.toString()) }
                    }
                })
                .handler { context: CommandContext<C> ->
                    val group = context.get<String>("group")
                    val id = context.get<Long>("id")
                    context.sender().sendMessage("Stopping service with ID $id from group $group")
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
                .optional("group", stringParser(), SuggestionProvider { _, _ ->
                    controllerApi.getGroups().getAllGroups().thenApply { groups ->
                        groups.map { group -> Suggestion.suggestion(group.name) }
                    }
                })
                .optional("id", longParser(), SuggestionProvider { _, _ ->
                    controllerApi.getServers().getAllServers().thenApply { servers ->
                        servers.map { server -> Suggestion.suggestion(server.numericalId.toString()) }
                    }
                })
                .handler { context: CommandContext<C> ->
                    val groupName = context.getOrDefault("group", null as String?)
                    val id = context.getOrDefault("id", null as Long?)

                    when {
                        groupName != null && id != null -> {
                            controllerApi.getServers().getServerByNumerical(groupName, id).thenAccept { server ->
                                context.sender().sendMessage("Server: ${server.group}")
                            }
                        }
                        groupName != null -> {
                            println("Getting servers from group $groupName")
                            controllerApi.getServers().getServersByGroup(groupName).thenAccept { servers ->
                                servers.forEach { server ->
                                    context.sender().sendMessage("Server: ${server.group}")
                                }
                            }
                        }
                        id != null -> {
                            println("Getting server with ID $id")
                        }
                        else -> {
                            println("Getting all servers.")
                            controllerApi.getServers().getAllServers().thenAccept { servers ->
                                servers.forEach { server ->
                                    context.sender().sendMessage("Server: ${server.group}")
                                }
                            }
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
                .optional("group", stringParser(), SuggestionProvider { _, _ ->
                    controllerApi.getGroups().getAllGroups().thenApply { groups ->
                        groups.map { group -> Suggestion.suggestion(group.name) }
                    }
                })
                .handler { context: CommandContext<C> ->
                    val groupName = context.getOrDefault("group", null as String?)
                    if (groupName != null) {
                        controllerApi.getGroups().getGroupByName(groupName).thenAccept { group ->
                            context.sender().sendMessage("Group: ${group.name}")
                        }
                    } else {
                        controllerApi.getGroups().getAllGroups().thenAccept { groups ->
                            groups.forEach { group ->
                                context.sender().sendMessage("Group: ${group.name}")
                            }
                        }
                    }
                }
                .build()
        )
    }
}
