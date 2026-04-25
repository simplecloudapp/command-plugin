package app.simplecloud.plugin.command.shared.config

import app.simplecloud.plugin.api.shared.config.AbstractMessageConfig
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Setting

@ConfigSerializable
data class MessageConfig(
    val version: String = "1",
    override val variables: Map<String, String> = mapOf(
        "prefix" to "<#0EA5E9><bold>SC</bold> <#475569>|"
    ),
    val command: CommandSection = CommandSection()
) : AbstractMessageConfig()

@ConfigSerializable
data class CommandSection(
    val format: FormatSection = FormatSection(),
    val help: HelpSection = HelpSection(),
    val usage: UsageSection = UsageSection(),
    val permission: PermissionSection = PermissionSection(),
    val group: GroupSection = GroupSection(),
    val server: ServerSection = ServerSection(),
    val player: PlayerSection = PlayerSection(),
    val reload: ReloadSection = ReloadSection(),
    val error: ErrorSection = ErrorSection()
)

@ConfigSerializable
data class FormatSection(
    @Setting("info-entry")
    val infoEntry: String = "   <#94A3B8><key><#475569>: <#E2E8F0><value>",
    @Setting("list-entry")
    val listEntry: String = "   <#475569>- <#F8FAFC><name> <#475569>(<#E2E8F0><value><#475569>)",
    @Setting("player-entry")
    val playerEntry: String = "   <#475569>- <white><head:<playername>:true> <#F8FAFC><displayname> <#475569>(<#E2E8F0><value><#475569>)"
)

@ConfigSerializable
data class HelpSection(
    val title: String = "<prefix> <#0EA5E9>SimpleCloud commands",
    val entry: String = "<#E2E8F0><command>",
    val empty: String = "<prefix> <#F59E0B>No commands are available for you."
)

@ConfigSerializable
data class UsageSection(
    val invalid: String = "<prefix> <#DC2626>Use <#F8FAFC><command> <#DC2626>instead.",
    val entry: String = "<#E2E8F0><command>",
    @Setting("missing-argument")
    val missingArgument: String = "<prefix> <#DC2626>Missing argument <#F8FAFC><argument><#DC2626>.",
    @Setting("invalid-number")
    val invalidNumber: String = "<prefix> <#DC2626><#F8FAFC><value> <#DC2626>is not a valid number.",
    @Setting("invalid-target-type")
    val invalidTargetType: String = "<prefix> <#DC2626>Use <#F8FAFC>group <#DC2626>or <#F8FAFC>ps <#DC2626>as target type."
)

@ConfigSerializable
data class PermissionSection(
    val denied: String = "<prefix> <#DC2626>You do not have permission to use this command."
)

@ConfigSerializable
data class GroupSection(
    val list: GroupListSection = GroupListSection(),
    val info: GroupInfoSection = GroupInfoSection(),
    val start: GroupStartSection = GroupStartSection(),
    val stop: GroupStopSection = GroupStopSection(),
    val error: GroupErrorSection = GroupErrorSection()
)

@ConfigSerializable
data class GroupListSection(
    val title: String = "<prefix> <#0EA5E9>Groups <#475569>(<#E2E8F0><count><#475569>)",
    val entry: String = "   <#475569>- <#F8FAFC><group> <#475569>(<#E2E8F0><value><#475569>)",
    val empty: String = "<prefix> <#F59E0B>No groups were found."
)

@ConfigSerializable
data class GroupInfoSection(
    val title: String = "<prefix> <#0EA5E9>Information about group <#F8FAFC><group>",
    val entry: String = "   <#94A3B8><key><#475569>: <#E2E8F0><value>"
)

@ConfigSerializable
data class GroupStartSection(
    val success: String = "<prefix> <#A3E635>Starting <#F8FAFC><count> servers <#A3E635>for group <#F8FAFC><group><#A3E635>.",
    val queued: String = "<prefix> <#0EA5E9>Start request for group <#F8FAFC><group> <#0EA5E9>was queued."
)

@ConfigSerializable
data class GroupStopSection(
    val success: String = "<prefix> <#A3E635>Stopping servers of group <#F8FAFC><group><#A3E635>.",
    @Setting("success-with-ids")
    val successWithIds: String = "<prefix> <#A3E635>Stopping servers <#F8FAFC><ids> <#A3E635>of group <#F8FAFC><group><#A3E635>."
)

@ConfigSerializable
data class GroupErrorSection(
    @Setting("not-found")
    val notFound: String = "<prefix> <#DC2626>Group <#F8FAFC><group> <#DC2626>was not found.",
    @Setting("already-empty")
    val alreadyEmpty: String = "<prefix> <#F59E0B>Group <#F8FAFC><group> <#F59E0B>has no running servers.",
    @Setting("start-failed")
    val startFailed: String = "<prefix> <#DC2626>Could not start servers for group <#F8FAFC><group><#DC2626>.",
    @Setting("stop-failed")
    val stopFailed: String = "<prefix> <#DC2626>Could not stop servers of group <#F8FAFC><group><#DC2626>."
)

@ConfigSerializable
data class ServerSection(
    val list: ServerListSection = ServerListSection(),
    val info: ServerInfoSection = ServerInfoSection(),
    val start: ServerStartSection = ServerStartSection(),
    val stop: ServerStopSection = ServerStopSection(),
    val error: ServerErrorSection = ServerErrorSection()
)

@ConfigSerializable
data class ServerListSection(
    val title: String = "<prefix> <#0EA5E9>Servers <#475569>(<#E2E8F0><count><#475569>)",
    val entry: String = "   <#475569>- <#F8FAFC><server> <#475569>(<#E2E8F0><value><#475569>)",
    val empty: String = "<prefix> <#F59E0B>No running servers were found."
)

@ConfigSerializable
data class ServerInfoSection(
    val title: String = "<prefix> <#0EA5E9>Information about server <#F8FAFC><server>",
    val entry: String = "   <#94A3B8><key><#475569>: <#E2E8F0><value>"
)

@ConfigSerializable
data class ServerStartSection(
    val success: String = "<prefix> <#A3E635>Starting server <#F8FAFC><group> <id><#A3E635>.",
    val queued: String = "<prefix> <#0EA5E9>Start request for server <#F8FAFC><group> <id> <#0EA5E9>was queued."
)

@ConfigSerializable
data class ServerStopSection(
    val success: String = "<prefix> <#A3E635>Stopping server <#F8FAFC><group> <id><#A3E635>."
)

@ConfigSerializable
data class ServerErrorSection(
    @Setting("not-found")
    val notFound: String = "<prefix> <#DC2626>Server <#F8FAFC><group> <id> <#DC2626>was not found.",
    @Setting("already-stopped")
    val alreadyStopped: String = "<prefix> <#F59E0B>Server <#F8FAFC><group> <id> <#F59E0B>is already stopped.",
    @Setting("start-failed")
    val startFailed: String = "<prefix> <#DC2626>Could not start server <#F8FAFC><group> <id><#DC2626>.",
    @Setting("stop-failed")
    val stopFailed: String = "<prefix> <#DC2626>Could not stop server <#F8FAFC><group> <id><#DC2626>."
)

@ConfigSerializable
data class PlayerSection(
    val list: PlayerListSection = PlayerListSection(),
    val info: PlayerInfoSection = PlayerInfoSection(),
    val send: PlayerSendSection = PlayerSendSection(),
    val message: PlayerMessageSection = PlayerMessageSection(),
    val error: PlayerErrorSection = PlayerErrorSection()
)

@ConfigSerializable
data class PlayerListSection(
    val title: String = "<prefix> <#0EA5E9>Players <#475569>(<#E2E8F0><count><#475569>)",
    val entry: String = "   <#475569>- <white><head:<playername>:true> <#F8FAFC><displayname> <#475569>(<#E2E8F0><value><#475569>)",
    val empty: String = "<prefix> <#F59E0B>No players were found for this target."
)

@ConfigSerializable
data class PlayerInfoSection(
    val title: String = "<prefix> <#0EA5E9>Information about <white><head:<playername>:true> <#F8FAFC><displayname>",
    val entry: String = "   <#94A3B8><key><#475569>: <#E2E8F0><value>"
)

@ConfigSerializable
data class PlayerSendSection(
    val success: String = "<prefix> <#A3E635>Sent <white><head:<playername>:true> <#F8FAFC><displayname> <#A3E635>to <#F8FAFC><target><#A3E635>.",
    @Setting("success-all")
    val successAll: String = "<prefix> <#A3E635>Sent <#F8FAFC><count> players <#A3E635>to <#F8FAFC><target><#A3E635>.",
    @Setting("success-from-server")
    val successFromServer: String = "<prefix> <#A3E635>Sent <#F8FAFC><count> players <#A3E635>from <#F8FAFC><source> <#A3E635>to <#F8FAFC><target><#A3E635>."
)

@ConfigSerializable
data class PlayerMessageSection(
    val success: String = "<prefix> <#A3E635>Message sent to <white><head:<playername>:true> <#F8FAFC><displayname><#A3E635>."
)

@ConfigSerializable
data class PlayerErrorSection(
    @Setting("not-found")
    val notFound: String = "<prefix> <#DC2626>Player <#F8FAFC><playername> <#DC2626>was not found.",
    @Setting("not-online")
    val notOnline: String = "<prefix> <#DC2626>Player <#F8FAFC><playername> <#DC2626>is not online.",
    @Setting("same-target")
    val sameTarget: String = "<prefix> <#F59E0B><white><head:<playername>:true> <#F8FAFC><displayname> <#F59E0B>is already on <#F8FAFC><target><#F59E0B>.",
    @Setting("target-not-found")
    val targetNotFound: String = "<prefix> <#DC2626>Target <#F8FAFC><target> <#DC2626>was not found.",
    @Setting("source-not-found")
    val sourceNotFound: String = "<prefix> <#DC2626>Source <#F8FAFC><source> <#DC2626>was not found.",
    @Setting("send-failed")
    val sendFailed: String = "<prefix> <#DC2626>Could not send <white><head:<playername>:true> <#F8FAFC><displayname> <#DC2626>to <#F8FAFC><target><#DC2626>.",
    @Setting("send-all-failed")
    val sendAllFailed: String = "<prefix> <#DC2626>Could not send players to <#F8FAFC><target><#DC2626>.",
    @Setting("no-source-players")
    val noSourcePlayers: String = "<prefix> <#F59E0B>No players were found on <#F8FAFC><source><#F59E0B>.",
    @Setting("message-empty")
    val messageEmpty: String = "<prefix> <#DC2626>Write a message for <#F8FAFC><playername><#DC2626>.",
    @Setting("message-failed")
    val messageFailed: String = "<prefix> <#DC2626>Could not send the message to <#F8FAFC><playername><#DC2626>."
)

@ConfigSerializable
data class ReloadSection(
    val success: String = "<prefix> <#A3E635>SimpleCloud Command was reloaded.",
    val failed: String = "<prefix> <#DC2626>SimpleCloud Command could not be reloaded."
)

@ConfigSerializable
data class ErrorSection(
    val internal: String = "<prefix> <#DC2626>An internal error occurred. Try again later.",
    @Setting("cloud-unavailable")
    val cloudUnavailable: String = "<prefix> <#DC2626>The cloud service is currently unavailable.",
    @Setting("target-unavailable")
    val targetUnavailable: String = "<prefix> <#DC2626>Target <#F8FAFC><target> <#DC2626>is currently unavailable."
)
