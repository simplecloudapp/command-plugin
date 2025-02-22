package app.simplecloud.plugin.command.shared.config

import org.spongepowered.configurate.objectmapping.ConfigSerializable

/**
 * @author Fynn Bauer in 2024
 */

@ConfigSerializable
data class MessageConfig(
    val serverStarting: String = "<color:#38bdf8><bold>⚡</bold></color> <color:#ffffff>A new server of group <group> is <color:#a3e635>starting <color:#a3a3a3>(<group> <id>)",
    val serverStopped: String = "<color:#38bdf8><bold>⚡</bold></color> <color:#ffffff>Server <group> <id> was <color:#dc2626>stopped.",
    val groupServerStopped: String = "<color:#38bdf8><bold>⚡</bold></color> <color:#ffffff>Servers of group <group> were <color:#dc2626>stopped.",
    val groupDeleted: String = "<color:#38bdf8><bold>⚡</bold></color> <color:#ffffff>Server group <group> was <color:#dc2626>deleted.",

    val cloudHelpTitle: String = "<color:#38bdf8><bold>⚡</bold></color> <color:#ffffff>Commands of Cloud Command Plugin",
    val cloudStartCommand: String = "   <color:#a3a3a3>/cloud start <serverGroup>",
    val cloudStopCommand: String = "   <color:#a3a3a3>/cloud stop <serverGroup> <Numerical ID>",
    val cloudServerInfoCommand: String = "   <color:#a3a3a3>/cloud info servers [serverGroup] [Numerical ID]",
    val cloudGroupInfoCommand: String = "   <color:#a3a3a3>/cloud info groups [groupName] [Numerical ID]",
    val cloudDeleteGroupCommand: String = "   <color:#a3a3a3>/cloud delete group <serverGroup>",

    val serverInfoTitle: String = "<color:#38bdf8><bold>⚡</bold></color> <color:#ffffff>Information of server <servergroup> <color:#a3e635>● <serveramount> Online",
    val serverInfoType: String = "   <color:#a3a3a3>Type: <color:#38bdf8><grouptype>",
    val serverInfoSoftware: String = "   <color:#a3a3a3>Software: <color:#38bdf8><groupsoftware>",
    val serverInfoMemory: String = "   <color:#a3a3a3>Memory: <color:#38bdf8><groupmemory>",
    val serverInfoPlayers: String = "   <color:#a3a3a3>Players: <color:#38bdf8><groupplayers>",

    val groupInfoTitle: String = "<color:#38bdf8><bold>⚡</bold></color> <color:#ffffff>Information of group <servergroup> <color:#a3e635>● <serveramount> Online",
    val groupInfoType: String = "   <color:#a3a3a3>Type: <color:#38bdf8><grouptype>",
    val groupInfoTemplate: String = "   <color:#a3a3a3>Template: <color:#38bdf8><grouptemplate>",
    val groupInfoMemory: String = "   <color:#a3a3a3>Memory: <color:#38bdf8><minmemory>-<maxmemory>",
    val groupInfoPlayers: String = "   <color:#a3a3a3>Players: <color:#38bdf8><maxplayers>",

    val groupsListTitle: String = "<color:#38bdf8><bold>⚡</bold></color> <color:#ffffff>List of every server group",
    val groupsListEntry: String = "   <color:#a3a3a3><servergroup> <color:#a3e635>● <onlinecount> Online <color:#737373>(<template>, <type>, <maxcount> MaxCount, <minmemory>-<maxmemory>MB)",

    val groupServerListTitle: String = "<color:#38bdf8><bold>⚡</bold></color> <color:#ffffff>List of every running server of group <serverGroup>",
    val groupServerListEntry: String = "   <color:#a3a3a3><servergroup> <numericalid> <color:#737373>(<onlineplayers>/<maxplayers> Players, <minmemory>/<maxmemory>MB, <state>)",

    val serverListTitle: String = "<color:#38bdf8><bold>⚡</bold></color> <color:#ffffff>List of every running server",
    val serverListEntry: String = "   <color:#a3a3a3><servergroup> <numericalid> <color:#737373>(<onlineplayers>/<maxplayers> Players, <minmemory>/<maxmemory>MB, <state>)"

)