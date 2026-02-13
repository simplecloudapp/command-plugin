package app.simplecloud.plugin.command.shared.config

import kotlinx.coroutines.*
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.spongepowered.configurate.kotlin.objectMapperFactory
import org.spongepowered.configurate.loader.ParsingException
import org.spongepowered.configurate.yaml.NodeStyle
import org.spongepowered.configurate.yaml.YamlConfigurationLoader
import java.io.File
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardWatchEventKinds
import java.util.concurrent.ConcurrentHashMap

open class YamlConfig(private val dirPath: String) {

    companion object {
        protected val logger: Logger = LogManager.getLogger(YamlConfig::class.java)
    }

    private val watchService = FileSystems.getDefault().newWatchService()
    private val reloadListeners = ConcurrentHashMap<String, MutableList<(Any) -> Unit>>()
    private val lastReload = ConcurrentHashMap<String, Long>()

    private var watcherJob: Job? = null

    init {
        startWatcher()
    }

    private fun buildNode(path: String?): Pair<org.spongepowered.configurate.CommentedConfigurationNode, YamlConfigurationLoader> {
        val file = File(if (path != null) "$dirPath/${path.lowercase()}.yml" else dirPath)

        if (!file.exists()) {
            file.parentFile.mkdirs()
            file.createNewFile()
        }

        val loader = YamlConfigurationLoader.builder()
            .path(file.toPath())
            .nodeStyle(NodeStyle.BLOCK)
            .defaultOptions { options ->
                options.serializers { builder ->
                    builder.registerAnnotatedObjects(objectMapperFactory())
                }
            }.build()
        return Pair(loader.load(), loader)
    }

    inline fun <reified T> load(path: String?): T? {
        return load(path, T::class.java)
    }

    @PublishedApi
    internal fun <T> load(path: String?, clazz: Class<T>): T? {
        return try {
            val node = buildNode(path).first
            val config = node.get(clazz)

            if (config == null) {
                logger.error("Config loaded as null: ${path ?: "default"}")
            } else {
                logger.info("Config loaded: ${path ?: "default"}")
            }

            config
        } catch (e: ParsingException) {
            logger.error("Failed to parse config file: ${path ?: "default"} ${e.message}")
            null
        } catch (e: Exception) {
            logger.error("Failed to load config: ${path ?: "default"} ${e.message}", e)
            null
        }
    }

    fun <T> save(path: String?, obj: T) {
        try {
            val pair = buildNode(path)
            pair.first.set(obj)
            pair.second.save(pair.first)
            logger.info("Config saved: ${path ?: "default"}")
        } catch (e: Exception) {
            logger.error("Failed to save config: $path ${e.message}", e)
        }
    }

    private fun startWatcher() {
        val directory = File(dirPath).toPath()

        if (!directory.toFile().exists()) {
            directory.toFile().mkdirs()
        }

        try {
            directory.register(
                watchService,
                StandardWatchEventKinds.ENTRY_MODIFY,
                StandardWatchEventKinds.ENTRY_CREATE
            )

            watcherJob = CoroutineScope(Dispatchers.IO).launch {
                logger.info("Config file watcher started for directory: $dirPath")

                while (isActive) {
                    try {
                        val key = watchService.take()
                        delay(150)

                        for (event in key.pollEvents()) {
                            val path = event.context() as? Path ?: continue
                            val resolvedPath = directory.resolve(path)

                            if (Files.isDirectory(resolvedPath) || !resolvedPath.toString().endsWith(".yml")) {
                                continue
                            }

                            handleFileChange(resolvedPath.toFile())
                        }

                        key.reset()

                    } catch (e: InterruptedException) {
                        logger.info("Config watcher interrupted")
                        break
                    } catch (e: Exception) {
                        logger.warn("Error in config watcher: ${e.message}")
                    }
                }
            }

        } catch (e: Exception) {
            logger.error("Could not start config file watcher: ${e.message}", e)
        }
    }

    private fun handleFileChange(file: File) {
        val cacheKey = file.nameWithoutExtension
        val now = System.currentTimeMillis()

        val last = lastReload[cacheKey]
        if (last != null && now - last < 300) return
        lastReload[cacheKey] = now

        logger.info("Config file changed, reloading: ${file.name}")

        val listeners = reloadListeners[cacheKey]
        if (listeners.isNullOrEmpty()) return

        try {
            val loader = YamlConfigurationLoader.builder()
                .path(file.toPath())
                .nodeStyle(NodeStyle.BLOCK)
                .defaultOptions { options ->
                    options.serializers { builder ->
                        builder.registerAnnotatedObjects(objectMapperFactory())
                    }
                }.build()

            val node = loader.load()

            listeners.forEach { listener ->
                try {
                    listener(node)
                } catch (e: Exception) {
                    logger.error("Error in reload listener for $cacheKey: ${e.message}", e)
                }
            }

            logger.info("Config reloaded successfully: $cacheKey")

        } catch (e: ParsingException) {
            logger.error("Failed to parse changed config file: $cacheKey ${e.message}")
        } catch (e: Exception) {
            logger.error("Failed to reload config: $cacheKey ${e.message}", e)
        }
    }

    fun close() {
        watcherJob?.cancel()
        try {
            watchService.close()
            logger.info("Config watcher closed")
        } catch (e: Exception) {
            logger.warn("Error closing watch service: ${e.message}")
        }
    }
}