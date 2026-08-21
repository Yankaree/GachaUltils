package me.asrielyankare.gachaultils.core

import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * Persistent storage for instance data.
 *
 * Stores instance metadata as JSON files. Each instance is saved as a separate file
 * to allow atomic updates. An index file tracks all instance IDs.
 *
 * Storage structure:
 * <storageRoot>/
 *   instances/
 *     index.json         - List of instance IDs
 *     0.json             - Instance 0 data
 *     1.json             - Instance 1 data
 *     ...
 */
object InstanceStorage {
    private val instanceMap = ConcurrentHashMap<Int, InstanceId>()
    private var storageRoot: File? = null
    private var initialized = false

    /**
     * Initialize storage with a root directory.
     * Must be called before any operations.
     */
    fun init(root: File) {
        storageRoot = root
        val instancesDir = File(root, "instances")
        instancesDir.mkdirs()
        initialized = true
        loadAll()
    }

    /**
     * Initialize with in-memory only storage (for testing).
     */
    fun initInMemory() {
        storageRoot = null
        initialized = true
    }

    fun addInstance(instance: InstanceId) {
        instanceMap[instance.id] = instance
        saveInstance(instance)
    }

    fun updateInstance(instance: InstanceId) {
        instanceMap[instance.id] = instance
        saveInstance(instance)
    }

    fun removeInstance(instanceId: Int) {
        instanceMap.remove(instanceId)
        deleteInstanceFile(instanceId)
    }

    fun getInstance(instanceId: Int): InstanceId? {
        return instanceMap[instanceId]
    }

    fun getAllInstances(): List<InstanceId> {
        return instanceMap.values.toList().sortedBy { it.id }
    }

    fun findByPackageName(packageName: String): List<InstanceId> {
        return instanceMap.values.filter { it.packageName == packageName }
    }

    fun getNextId(): Int {
        return if (instanceMap.isEmpty()) 0 else (instanceMap.keys.max() ?: 0) + 1
    }

    private fun saveInstance(instance: InstanceId) {
        val root = storageRoot ?: return
        val file = File(root, "instances/${instance.id}.json")
        file.parentFile?.mkdirs()
        file.writeText(serializeInstance(instance))
    }

    private fun deleteInstanceFile(instanceId: Int) {
        val root = storageRoot ?: return
        val file = File(root, "instances/$instanceId.json")
        if (file.exists()) file.delete()
    }

    private fun loadAll() {
        val root = storageRoot ?: return
        val instancesDir = File(root, "instances")
        if (!instancesDir.exists()) return

        instanceMap.clear()
        instancesDir.listFiles()?.filter { it.extension == "json" }?.forEach { file ->
            try {
                val instance = deserializeInstance(file.readText())
                if (instance != null) {
                    instanceMap[instance.id] = instance
                }
            } catch (e: Exception) {
                // Skip corrupted files
            }
        }
    }

    /**
     * Simple JSON serialization without external dependencies.
     */
    private fun serializeInstance(instance: InstanceId): String {
        return buildString {
            append("{")
            append("\"id\":${instance.id},")
            append("\"userId\":${instance.userId},")
            append("\"packageName\":\"${escapeJson(instance.packageName)}\",")
            append("\"gamePackageName\":\"${escapeJson(instance.gameId.packageName)}\",")
            append("\"gameType\":\"${instance.gameId.gameType}\",")
            append("\"displayName\":\"${escapeJson(instance.displayName)}\",")
            append("\"state\":\"${instance.state}\",")
            append("\"apkPath\":\"${escapeJson(instance.apkPath)}\",")
            append("\"createdAt\":${instance.createdAt},")
            append("\"updatedAt\":${instance.updatedAt}")
            append("}")
        }
    }

    private fun deserializeInstance(json: String): InstanceId? {
        return try {
            val map = parseJson(json)
            InstanceId(
                id = map["id"]?.toIntOrNull() ?: return null,
                userId = map["userId"]?.toIntOrNull() ?: return null,
                packageName = map["packageName"] ?: return null,
                gameId = GameId(
                    packageName = map["gamePackageName"] ?: return null,
                    gameType = GameType.valueOf(map["gameType"] ?: "UNSPECIFIED")
                ),
                displayName = map["displayName"] ?: "Instance",
                state = try {
                    InstanceState.valueOf(map["state"] ?: "CREATED")
                } catch (e: Exception) { InstanceState.CREATED },
                apkPath = map["apkPath"] ?: "",
                createdAt = map["createdAt"]?.toLongOrNull() ?: System.currentTimeMillis(),
                updatedAt = map["updatedAt"]?.toLongOrNull() ?: System.currentTimeMillis()
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun escapeJson(str: String): String {
        return str.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }

    private fun parseJson(json: String): Map<String, String> {
        val map = mutableMapOf<String, String>()
        val cleaned = json.trim().removePrefix("{").removeSuffix("}")
        val pairs = cleaned.split(",")
        for (pair in pairs) {
            val colonIndex = pair.indexOf(':')
            if (colonIndex < 0) continue
            val key = pair.substring(0, colonIndex).trim().removeSurrounding("\"")
            val value = pair.substring(colonIndex + 1).trim()
                .removeSurrounding("\"")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\")
                .replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\t", "\t")
            map[key] = value
        }
        return map
    }

    fun clearAll() {
        instanceMap.clear()
    }
}
