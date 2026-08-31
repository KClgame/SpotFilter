package kcl.spotfilter.client.config

import kcl.spotfilter.client.data.SpotKind
import kcl.spotfilter.client.filter.AutoPin
import kcl.spotfilter.client.filter.AutoPinRule
import kcl.spotfilter.client.filter.FilterState
import net.fabricmc.loader.api.FabricLoader
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path

class RulePack(
	val id: String,
	val builtin: Boolean,
	var enabled: Boolean,
	val normal: MutableList<AutoPinRule> = ArrayList(),
	val grotto: MutableList<AutoPinRule> = ArrayList()
) {
	fun rules(kind: SpotKind): MutableList<AutoPinRule> =
		if (kind == SpotKind.GROTTO) grotto else normal
}

object RulePacks {
	const val DEFAULT_ID = "default"
	const val BLANK_ID = "blank"
	val TYPE_IDS = listOf("fish", "pearl", "treasure", "spirit", "xp_wayfinder")
	private val builtins = TYPE_IDS + BLANK_ID

	private val root = FabricLoader.getInstance().configDir.resolve("spotfilter")
	val packsDir: Path = root.resolve("packs")
	val exportDir: Path = root.resolve("export")

	val packs: MutableList<RulePack> = ArrayList()
	var lastErrors: List<String> = emptyList()
		private set
	var lastMessage: String = ""
		private set

	fun enabledIds(): List<String> = packs.filter { it.enabled }.map { it.id }

	fun byId(id: String): RulePack? = packs.firstOrNull { it.id.equals(id, ignoreCase = true) }

	fun loadAll(enabled: List<String>?) {
		Files.createDirectories(packsDir)
		ensureBuiltinFiles()
		migrateLegacyRules()
		val enabledSet = resolveEnabled(enabled)
		val loaded = LinkedHashMap<String, RulePack>()
		for (id in builtins) {
			loaded[id] = readPack(id, builtin = true, enabled = id.lowercase() in enabledSet)
		}
		if (!Files.isDirectory(packsDir)) {
			packs.clear()
			packs.addAll(loaded.values)
			syncToFilterState()
			return
		}
		Files.list(packsDir).use { stream ->
			stream.filter { Files.isRegularFile(it) && it.fileName.toString().endsWith(".txt", true) }
				.forEach { file ->
					val (id, grottoFile) = splitPackFileName(file.fileName.toString()) ?: return@forEach
					if (id in loaded && loaded[id]!!.builtin) return@forEach
					val pack = loaded.getOrPut(id) {
						RulePack(id, builtin = false, enabled = id.lowercase() in enabledSet)
					}
					val parsed = readFile(file, if (grottoFile) SpotKind.GROTTO else SpotKind.NORMAL)
					if (grottoFile) {
						pack.grotto.clear()
						pack.grotto.addAll(parsed.grotto.ifEmpty { parsed.normal })
					} else {
						pack.normal.clear()
						pack.normal.addAll(parsed.normal)
						if (parsed.grotto.isNotEmpty() && pack.grotto.isEmpty()) {
							pack.grotto.addAll(parsed.grotto)
						}
					}
				}
		}
		packs.clear()
		packs.addAll(loaded.values)
		syncToFilterState()
	}

	fun syncToFilterState() {
		FilterState.normal.autoPinRules.clear()
		FilterState.grotto.autoPinRules.clear()
		for (pack in packs.filter { it.enabled }) {
			FilterState.normal.autoPinRules.addAll(pack.normal)
			FilterState.grotto.autoPinRules.addAll(pack.grotto)
		}
	}

	fun savePack(pack: RulePack) {
		Files.createDirectories(packsDir)
		Files.writeString(normalPath(pack.id), RulesFile.formatKind(SpotKind.NORMAL, pack.normal), StandardCharsets.UTF_8)
		Files.writeString(grottoPath(pack.id), RulesFile.formatKind(SpotKind.GROTTO, pack.grotto), StandardCharsets.UTF_8)
		lastMessage = "Saved ${pack.id}.txt and ${pack.id}_grotto.txt"
	}

	fun create(rawName: String): RulePack? {
		val id = sanitizeId(rawName) ?: return null
		if (byId(id) != null) {
			lastMessage = "Pack '$id' already exists"
			return byId(id)
		}
		val pack = RulePack(id, builtin = false, enabled = false)
		packs.add(pack)
		savePack(pack)
		lastMessage = "Created pack '$id'"
		return pack
	}

	fun delete(pack: RulePack): Boolean {
		if (pack.builtin) {
			lastMessage = "Cannot delete builtin pack '${pack.id}'"
			return false
		}
		packs.remove(pack)
		Files.deleteIfExists(normalPath(pack.id))
		Files.deleteIfExists(grottoPath(pack.id))
		syncToFilterState()
		lastMessage = "Deleted pack '${pack.id}'"
		return true
	}

	fun importFile(rawPath: String): RulePack? {
		val source = resolveImportPath(rawPath) ?: run {
			lastMessage = "File not found: $rawPath"
			return null
		}
		val fileName = source.fileName.toString()
		val (id, grottoFile) = splitPackFileName(fileName) ?: run {
			lastMessage = "Need a .txt file"
			return null
		}
		val parsed = readFile(source, if (grottoFile) SpotKind.GROTTO else SpotKind.NORMAL)
		val pack = byId(id) ?: RulePack(id, builtin = id in builtins, enabled = false).also { packs.add(it) }
		if (grottoFile) {
			pack.grotto.clear()
			pack.grotto.addAll(parsed.grotto.ifEmpty { parsed.normal })
		} else {
			pack.normal.clear()
			pack.normal.addAll(parsed.normal)
			if (parsed.grotto.isNotEmpty()) {
				pack.grotto.clear()
				pack.grotto.addAll(parsed.grotto)
			}
		}
		savePack(pack)
		syncToFilterState()
		lastMessage = "Loaded ${source.fileName} into pack '${pack.id}'"
		return pack
	}

	fun exportPack(pack: RulePack): Path {
		Files.createDirectories(exportDir)
		val normalOut = exportDir.resolve("${pack.id}.txt")
		val grottoOut = exportDir.resolve("${pack.id}_grotto.txt")
		Files.writeString(normalOut, RulesFile.formatKind(SpotKind.NORMAL, pack.normal), StandardCharsets.UTF_8)
		Files.writeString(grottoOut, RulesFile.formatKind(SpotKind.GROTTO, pack.grotto), StandardCharsets.UTF_8)
		savePack(pack)
		lastMessage = "Exported to config/spotfilter/export/${pack.id}.txt and ${pack.id}_grotto.txt"
		return exportDir
	}

	fun toggle(pack: RulePack) {
		pack.enabled = !pack.enabled
		syncToFilterState()
		AutoPin.applyAll()
	}

	fun groupingName(rule: AutoPinRule): String =
		rule.nickname.trim().ifBlank { rule.name.trim() }.ifBlank { "Rule" }

	private fun resolveEnabled(enabled: List<String>?): Set<String> {
		if (enabled == null) return TYPE_IDS.toSet()
		val set = enabled.map { it.lowercase() }.filter { it.isNotBlank() }.toSet()
		if (set == setOf(DEFAULT_ID) || set == setOf(DEFAULT_ID, BLANK_ID)) {
			return TYPE_IDS.toSet()
		}
		return set
	}

	private fun ensureBuiltinFiles() {
		Files.createDirectories(packsDir)
		for (id in builtins) {
			seedBuiltinFile(normalPath(id), "$id.txt", emptyPackText(id, SpotKind.NORMAL))
			seedBuiltinFile(grottoPath(id), "${id}_grotto.txt", emptyPackText(id, SpotKind.GROTTO))
		}
	}

	private fun seedBuiltinFile(path: Path, resourceName: String, fallback: String) {
		val shipped = readBuiltinResource(resourceName)
		if (!Files.exists(path)) {
			Files.writeString(path, shipped ?: fallback, StandardCharsets.UTF_8)
			return
		}
		if (shipped != null && isEmptyPlaceholder(path)) {
			Files.writeString(path, shipped, StandardCharsets.UTF_8)
		}
	}

	private fun isEmptyPlaceholder(path: Path): Boolean {
		val text = Files.readString(path, StandardCharsets.UTF_8)
		return text.lineSequence().none { it.trim().startsWith("name=", ignoreCase = true) }
	}

	private fun readBuiltinResource(fileName: String): String? {
		val stream = RulePacks::class.java.getResourceAsStream("/assets/spotfilter/packs/$fileName")
			?: return null
		return stream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
	}

	private fun migrateLegacyRules() {
		val legacy = root.resolve("rules.txt")
		if (!Files.exists(legacy)) return
		val imported = packsDir.resolve("legacy.txt")
		if (Files.exists(imported) || Files.exists(packsDir.resolve("legacy_grotto.txt"))) return
		val parsed = readFile(legacy, SpotKind.NORMAL)
		val pack = RulePack("legacy", builtin = false, enabled = false)
		pack.normal.addAll(parsed.normal)
		pack.grotto.addAll(parsed.grotto)
		savePack(pack)
	}

	private fun readPack(id: String, builtin: Boolean, enabled: Boolean): RulePack {
		val pack = RulePack(id, builtin, enabled)
		val normalFile = normalPath(id)
		val grottoFile = grottoPath(id)
		if (Files.exists(normalFile)) {
			val parsed = readFile(normalFile, SpotKind.NORMAL)
			pack.normal.addAll(parsed.normal)
			if (parsed.grotto.isNotEmpty()) pack.grotto.addAll(parsed.grotto)
		}
		if (Files.exists(grottoFile)) {
			val parsed = readFile(grottoFile, SpotKind.GROTTO)
			pack.grotto.clear()
			pack.grotto.addAll(parsed.grotto.ifEmpty { parsed.normal })
		}
		return pack
	}

	private fun readFile(file: Path, defaultKind: SpotKind): RulesLoadResult {
		val text = Files.readString(file, StandardCharsets.UTF_8).removePrefix("\uFEFF")
		val result = RulesFile.parse(text, defaultKind)
		if (result.errors.isNotEmpty()) {
			lastErrors = result.errors.map { "${file.fileName}: $it" }
		}
		return result
	}

	private fun emptyPackText(id: String, kind: SpotKind): String = buildString {
		appendLine("# SpotFilter pack: $id")
		appendLine(if (kind == SpotKind.GROTTO) "# Grotto Auto Pin rules" else "# Normal island Auto Pin rules")
		appendLine("# (empty)")
		appendLine()
		appendLine(if (kind == SpotKind.GROTTO) "[grotto]" else "[normal]")
		appendLine("# (no rules)")
	}

	fun normalPath(id: String): Path = packsDir.resolve("$id.txt")
	fun grottoPath(id: String): Path = packsDir.resolve("${id}_grotto.txt")

	private fun splitPackFileName(fileName: String): Pair<String, Boolean>? {
		if (!fileName.endsWith(".txt", true)) return null
		val stem = fileName.dropLast(4)
		return if (stem.endsWith("_grotto", true)) {
			val id = sanitizeId(stem.dropLast("_grotto".length)) ?: return null
			id to true
		} else {
			val id = sanitizeId(stem) ?: return null
			id to false
		}
	}

	fun sanitizeId(raw: String): String? {
		val id = raw.trim().lowercase().replace(Regex("[^a-z0-9_-]+"), "_").trim('_')
		if (id.isEmpty() || id == "grotto") return null
		return id.removeSuffix("_grotto").ifBlank { null }
	}

	private fun resolveImportPath(raw: String): Path? {
		val text = raw.trim().removeSurrounding("\"").removeSurrounding("'")
		if (text.isEmpty()) return null
		val direct = Path.of(text)
		if (Files.isRegularFile(direct)) return direct
		val inPacks = packsDir.resolve(text)
		if (Files.isRegularFile(inPacks)) return inPacks
		val inRoot = root.resolve(text)
		if (Files.isRegularFile(inRoot)) return inRoot
		return null
	}
}
