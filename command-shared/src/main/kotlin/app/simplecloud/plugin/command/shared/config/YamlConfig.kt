package app.simplecloud.plugin.command.shared.config

import kotlinx.coroutines.*
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.spongepowered.configurate.CommentedConfigurationNode
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
    private val configCache = ConcurrentHashMap<String, CachedConfig>()
    private val reloadListeners = ConcurrentHashMap<String, MutableList<(Any) -> Unit>>()
    private val lastReload = ConcurrentHashMap<String, Long>()

    private var watcherJob: Job? = null

    data class CachedConfig(
        val data: Any,
        val timestamp: Long = System.currentTimeMillis()
    )

    init {
        startWatcher()
    }

    private fun buildNode(path: String?): Pair<CommentedConfigurationNode, YamlConfigurationLoader> {
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

    inline fun <reified T> getCached(path: String?): T? {
        return getCached(path, T::class.java)
    }

    @PublishedApi
    internal fun <T> getCached(path: String?, clazz: Class<T>): T? {
        val cacheKey = path ?: "default"
        val cached = configCache[cacheKey]

        @Suppress("UNCHECKED_CAST")
        return if (cached != null) {
            cached.data as? T
        } else {
            load(path, clazz)
        }
    }

    @PublishedApi
    internal fun <T> load(path: String?, clazz: Class<T>): T? {
        val cacheKey = path ?: "default"
        val cached = configCache[cacheKey]

        return try {
            val node = buildNode(path).first
            val config = node.get(clazz)

            if (config != null) {
                configCache[cacheKey] = CachedConfig(config)
                logger.info("Config loaded: $cacheKey")
                config
            } else {
                @Suppress("UNCHECKED_CAST")
                cached?.data as? T
            }

        } catch (e: ParsingException) {
            logger.error("Failed to parse config file: $cacheKey - ${e.message}")
            @Suppress("UNCHECKED_CAST")
            cached?.data as? T
        } catch (e: Exception) {
            logger.error("Failed to load config: $cacheKey - ${e.message}", e)
            @Suppress("UNCHECKED_CAST")
            cached?.data as? T
        }
    }

    fun <T> save(path: String?, obj: T) {
        try {
            val pair = buildNode(path)
            pair.first.set(obj)
            pair.second.save(pair.first)

            val cacheKey = path ?: "default"
            if (obj != null) {
                configCache[cacheKey] = CachedConfig(obj as Any)
            }

            logger.info("Config saved: $cacheKey")
        } catch (e: Exception) {
            logger.error("Failed to save config: $path - ${e.message}", e)
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
        if (last != null && now - last < 300) {
            return
        }
        lastReload[cacheKey] = now

        logger.info("Config file changed, reloading: ${file.name}")

        val oldCache = configCache[cacheKey] ?: return

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
            val newConfig = node.get(oldCache.data::class.java)

            if (newConfig != null) {
                configCache[cacheKey] = CachedConfig(newConfig)
                logger.info("Config reloaded successfully: $cacheKey")

                reloadListeners[cacheKey]?.forEach {
                    try {
                        it(newConfig)
                    } catch (e: Exception) {
                        logger.error("Error in reload listener: ${e.message}", e)
                    }
                }
            } else {
                logger.warn("Reloaded config is null, keeping cached version: $cacheKey")
            }
        } catch (e: ParsingException) {
            logger.error("Failed to parse changed config file, keeping cached version: $cacheKey", e)
            throw e
        } catch (e: Exception) {
            logger.error("Failed to reload config, keeping cached version: $cacheKey", e)
            throw e
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