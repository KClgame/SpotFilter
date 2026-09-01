package kcl.spotfilter.client.ui

import kcl.spotfilter.client.config.RulePack
import kcl.spotfilter.client.config.RulePacks
import kcl.spotfilter.client.config.SpotFilterConfig
import kcl.spotfilter.client.filter.FilterState
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.KeyEvent
import net.minecraft.network.chat.Component
import org.lwjgl.glfw.GLFW

class ConfigPacksScreen(
	private val returnTo: Screen
) : Screen(Component.literal("Auto Pin configs")) {
	private lateinit var nameBox: EditBox
	private lateinit var fileBox: EditBox

	override fun isPauseScreen(): Boolean = false

	override fun init() {
		val kind = FilterState.kind
		val modeRules = RulePacks.packs.sumOf { it.rules(kind).size }
		addRenderableWidget(
			Button.builder(Component.literal("${kind.label}  (${modeRules} rules)")) { _ ->
				FilterState.toggleKind()
				SpotFilterConfig.save()
				rebuildWidgets()
			}.bounds(8, 6, 180, 20).build()
		)
		var y = 32
		RulePacks.packs.forEach { pack ->
			val row = y
			addRenderableWidget(
				Button.builder(Component.literal(if (pack.enabled) "On" else "Off")) { _ ->
					RulePacks.toggle(pack)
					SpotFilterConfig.save()
					rebuildWidgets()
				}.bounds(8, row, 36, 20).build()
			)
			addRenderableWidget(
				Button.builder(Component.literal(packLabel(pack))) { _ ->
					minecraft.gui.setScreen(AutoPinListScreen(this, pack))
				}.bounds(48, row, width - 248, 20).build()
			)
			addRenderableWidget(
				Button.builder(Component.literal("Edit")) { _ ->
					minecraft.gui.setScreen(AutoPinListScreen(this, pack))
				}.bounds(width - 192, row, 48, 20).build()
			)
			addRenderableWidget(
				Button.builder(Component.literal("Export")) { _ ->
					RulePacks.exportPack(pack)
					rebuildWidgets()
				}.bounds(width - 140, row, 60, 20).build()
			)
			addRenderableWidget(
				Button.builder(Component.literal("Del")) { _ ->
					RulePacks.delete(pack)
					SpotFilterConfig.save()
					rebuildWidgets()
				}.bounds(width - 76, row, 68, 20).build()
			)
			y += 24
		}

		val boxY = (y + 8).coerceAtMost(height - 78)
		nameBox = EditBox(font, 8, boxY, width - 140, 20, Component.literal("Name"))
		nameBox.setMaxLength(32)
		nameBox.setHint(Component.literal("new pack name"))
		addRenderableWidget(nameBox)
		addRenderableWidget(
			Button.builder(Component.literal("Create")) { _ ->
				RulePacks.create(nameBox.value)
				SpotFilterConfig.save()
				rebuildWidgets()
			}.bounds(width - 124, boxY, 116, 20).build()
		)

		fileBox = EditBox(font, 8, boxY + 24, width - 140, 20, Component.literal("File"))
		fileBox.setMaxLength(256)
		fileBox.setHint(Component.literal("path or packs/name.txt"))
		addRenderableWidget(fileBox)
		addRenderableWidget(
			Button.builder(Component.literal("Load file")) { _ ->
				RulePacks.importFile(fileBox.value)
				SpotFilterConfig.save()
				rebuildWidgets()
			}.bounds(width - 124, boxY + 24, 116, 20).build()
		)

		addRenderableWidget(
			Button.builder(Component.literal("Back")) { _ -> onClose() }
				.bounds(width / 2 - 50, height - 28, 100, 20).build()
		)
	}

	private fun packLabel(pack: RulePack): String {
		val n = pack.rules(FilterState.kind).size
		return "${pack.id}  ${FilterState.kind.label}: $n rules"
	}

	override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
		super.extractRenderState(graphics, mouseX, mouseY, delta)
		graphics.text(
			font,
			Component.literal("Showing ${FilterState.kind.label} rules only. Check several packs to run in parallel."),
			196,
			10,
			0xFFFFFFFF.toInt(),
			false
		)
		graphics.text(
			font,
			Component.literal(
				RulePacks.lastMessage.ifBlank {
					"Normal: packs/<name>.txt   Grotto: packs/<name>_grotto.txt   Export: config/spotfilter/export/"
				}
			),
			8,
			height - 42,
			0xFFAAAAAA.toInt(),
			false
		)
	}

	override fun keyPressed(event: KeyEvent): Boolean {
		if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
			onClose()
			return true
		}
		if (super.keyPressed(event)) return true
		if (typingInBox()) return true
		if (event.key() == GLFW.GLFW_KEY_O) {
			onClose()
			return true
		}
		return false
	}

	override fun onClose() {
		SpotFilterConfig.save()
		RulePacks.syncToFilterState()
		minecraft.gui.setScreen(returnTo)
	}
}
