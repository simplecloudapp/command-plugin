package app.simplecloud.plugin.command.shared.config

import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
data class MessageConfig(
    val cloudHelpMessage: String = """
        <color:#38bdf8><bold>⚡</bold></color> <color:#ffffff>Commands of Cloud Command Plugin
           <color:#a3a3a3>/cloud start group <group>
           <color:#a3a3a3>/cloud start persistent <server>
           <color:#a3a3a3>/cloud stop group <group> [id]
           <color:#a3a3a3>/cloud stop persistent <server>
           <color:#a3a3a3>/cloud info servers [group] [id]
           <color:#a3a3a3>/cloud info groups [group]
           <color:#a3a3a3>/cloud info persistent [server]
           <color:#a3a3a3>/cloud delete group <group>
           <color:#a3a3a3>/cloud delete persistent <server>
           <color:#a3a3a3>/cloud edit group <group> <key> <value>
           <color:#a3a3a3>/cloud edit server <group> <id> <key> <value>
           <color:#a3a3a3>/cloud edit persistent <server> <key> <value>
    """.trimIndent(),

    val serverStarting: String = "<color:#38bdf8><bold>⚡</bold></color> <color:#ffffff>A new server of group <group_name> is <color:#a3e635>starting.",
    val persistentServerActivated: String = "<color:#38bdf8><bold>⚡</bold></color> <color:#ffffff>Persistent server <persistent_name> was <color:#a3e635>activated.",

    val serverStopped: String = "<color:#38bdf8><bold>⚡</bold></color> <color:#ffffff>Server <server_display_name> was <color:#dc2626>stopped.",
    val groupServersStopped: String = "<color:#38bdf8><bold>⚡</bold></color> <color:#ffffff>All servers of group <group_name> were <color:#dc2626>stopped.",
    val persistentServerDeactivated: String = "<color:#38bdf8><bold>⚡</bold></color> <color:#ffffff>Persistent server <persistent_name> was <color:#dc2626>deactivated.",

    val groupDeleted: String = "<color:#38bdf8><bold>⚡</bold></color> <color:#ffffff>Server group <group_name> was <color:#dc2626>deleted.",
    val persistentServerDeleted: String = "<color:#38bdf8><bold>⚡</bold></color> <color:#ffffff>Persistent server <persistent_name> was <color:#dc2626>deleted.",

    val serverInfoTitle: String = "<color:#38bdf8><bold>⚡</bold></color> <color:#ffffff>Information of server <server_display_name>",
    val serverInfoState: String = "   <color:#a3a3a3>State: <color:#38bdf8><server_state>",
    val serverInfoMemory: String = "   <color:#a3a3a3>Memory: <color:#38bdf8><server_min_memory>-<server_max_memory> MB",
    val serverInfoPlayers: String = "   <color:#a3a3a3>Players: <color:#38bdf8><server_players>/<server_max_players>",

    val groupInfoTitle: String = "<color:#38bdf8><bold>⚡</bold></color> <color:#ffffff>Information of group <group_name> <color:#a3e635> <server_count> Online",
    val groupInfoType: String = "   <color:#a3a3a3>Type: <color:#38bdf8><group_type>",
    val groupInfoMemory: String = "   <color:#a3a3a3>Memory: <color:#38bdf8><group_min_memory>-<group_max_memory> MB",
    val groupInfoPlayers: String = "   <color:#a3a3a3>Max Players: <color:#38bdf8><group_max_players>",

    val persistentServerInfoTitle: String = "<color:#38bdf8><bold>⚡</bold></color> <color:#ffffff>Information of persistent server <persistent_name>",
    val persistentServerInfoType: String = "   <color:#a3a3a3>Type: <color:#38bdf8><persistent_type>",
    val persistentServerInfoMemory: String = "   <color:#a3a3a3>Memory: <color:#38bdf8><persistent_min_memory>-<persistent_max_memory> MB",
    val persistentServerInfoPlayers: String = "   <color:#a3a3a3>Max Players: <color:#38bdf8><persistent_max_players>",
    val persistentServerInfoActive: String = "   <color:#a3a3a3>Active: <color:#38bdf8><persistent_active>",
    val persistentServerInfoServerhost: String = "   <color:#a3a3a3>Serverhost: <color:#38bdf8><persistent_serverhost>",

    val serverListTitle: String = "<color:#38bdf8><bold>⚡</bold></color> <color:#ffffff>List of every running server",
    val serverListEntry: String = "   <color:#a3a3a3><server_display_name> <color:#737373>(<server_players>/<server_max_players> Players, <server_min_memory>/<server_max_memory>MB, <server_state>)",

    val groupsListTitle: String = "<color:#38bdf8><bold>⚡</bold></color> <color:#ffffff>List of every server group",
    val groupsListEntry: String = "   <color:#a3a3a3><group_name> <color:#a3e635> <server_count> Online <color:#737373>(<group_type>, <group_min_memory>-<group_max_memory>MB, <group_max_players> MaxPlayers)",

    val groupServerListTitle: String = "<color:#38bdf8><bold>⚡</bold></color> <color:#ffffff>List of every running server of group <group_name>",
    val groupServerListEntry: String = "   <color:#a3a3a3><server_display_name> <color:#737373>(<server_players>/<server_max_players> Players, <server_min_memory>/<server_max_memory>MB, <server_state>)",

    val persistentServersListTitle: String = "<color:#38bdf8><bold>⚡</bold></color> <color:#ffffff>List of every persistent server",
    val persistentServersListEntry: String = "   <color:#a3a3a3><persistent_name> <color:#a3e635> <persistent_active> <color:#737373>(<persistent_type>, <persistent_min_memory>-<persistent_max_memory>MB, <persistent_max_players> MaxPlayers)",

    val groupUpdated: String = "<color:#38bdf8><bold>⚡</bold></color> <color:#ffffff>Group <group_name> was <color:#a3e635>updated.",
    val serverUpdated: String = "<color:#38bdf8><bold>⚡</bold></color> <color:#ffffff>Server <server_display_name> was <color:#a3e635>updated.",
    val persistentServerUpdated: String = "<color:#38bdf8><bold>⚡</bold></color> <color:#ffffff>Persistent server <persistent_name> was <color:#a3e635>updated.",

    val invalidValue: String = "<color:#38bdf8><bold>⚡</bold></color> <color:#dc2626>Invalid value <color:#a3a3a3><value> <color:#dc2626>for <color:#a3a3a3><key>.",
    val invalidSetting: String = "<color:#38bdf8><bold>⚡</bold></color> <color:#dc2626>Invalid setting <color:#a3a3a3><key>.",
    val errorMessage: String = "<color:#38bdf8><bold>⚡</bold></color> <color:#dc2626>Error: <color:#a3a3a3><error>",
)
