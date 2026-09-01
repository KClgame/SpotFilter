package kcl.spotfilter.client.ui

import kcl.spotfilter.client.config.SpotFilterConfig
import kcl.spotfilter.client.data.SpotKind
import kcl.spotfilter.client.data.SpotPool
import kcl.spotfilter.client.filter.FilterState
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.Tooltip
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.network.chat.CommonComponents
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import org.lwjgl.glfw.GLFW

class FilterScreen : Screen(Component.literal("SpotFilter")) {
	private var editingHud = false
	private var scroll = 0
	private var dragging = false
	private var dragOffX = 0
	private var dragOffY = 0

	private val compact get() = SpotFilterConfig.instance.layout() == HudLayout.COMPACT
	private val listTop get() = 136
	private val listBottom get() = height - 36
	private val rowHeight get() = if (compact) 22 else 44

	override fun isPauseScreen(): Boolean = false

	override fun init() {
		fillButtons()
	}

	private fun fillButtons() {
		if (editingHud) {
			addRenderableWidget(
				Button.builder(Component.literal("Done")) { _ ->
					editingHud = false
					SpotFilterConfig.save()
					rebuildWidgets()
				}.bounds(width / 2 - 50, height - 28, 100, 20).build()
			)
			return
		}

		val cfg = SpotFilterConfig.instance
		addTopRow(
			8,
			TopBtn(
				kickLabel(),
				tip(
					"Auto-kick Depleted spots.",
					"On: unpin and drop a spot when Stock becomes Depleted, including manual pins.",
					"Off: keep Depleted spots pinned; they stay until you unpin or Clear."
				)
			) { _ ->
				cfg.kickDepleted = !cfg.kickDepleted
				if (cfg.kickDepleted) SpotPool.kickDepletedNow()
				SpotFilterConfig.save()
				rebuildWidgets()
			},
			TopBtn(
				kindLabel(),
				tip(
					"Island Normal spots vs Grotto (Stability Cost) spots.",
					"Filters, Pair, and Auto Pin are separate per mode.",
					"The other group is hidden from the list, HUD, and guides."
				)
			) { _ ->
				FilterState.toggleKind()
				SpotFilterConfig.save()
				rebuildWidgets()
			},
			TopBtn(
				modeLabel(),
				tip(
					"How F1–F3 combine.",
					"AND: a spot must match every filled slot.",
					"OR: a spot matches if any filled slot matches. Empty slots are ignored."
				)
			) { _ ->
				FilterState.toggleMode()
				SpotFilterConfig.save()
				rebuildWidgets()
			},
			TopBtn(
				layoutLabel(),
				tip(
					"HUD and list text layout.",
					"Compact: one line per spot with small perk icons.",
					"Detailed: title plus indented perk rows."
				)
			) { _ ->
				cfg.setLayout(cfg.layout().toggle())
				SpotFilterConfig.save()
				rebuildWidgets()
			},
			TopBtn(
				Component.literal("Edit HUD"),
				tip(
					"Move and style the coordinate HUD.",
					"Drag to reposition. Scroll to scale (0.5x–3.0x).",
					"Shift+scroll changes background opacity."
				)
			) { _ ->
				editingHud = true
				rebuildWidgets()
			},
			TopBtn(
				Component.literal("Clear"),
				tip(
					"Clear the scanned spot pool (same as P).",
					"HUD, filters, Auto Pin rules, and Enabled stay.",
					"Does not wait for the hourly or Grotto chat refresh."
				)
			) { _ ->
				SpotPool.clearSpots()
			},
			TopBtn(
				enableLabel(),
				tip(
					"Master overlay switch.",
					"Disabled: hide HUD and world guides, mute new-spot sound.",
					"Scanning and this Filter screen still work. L only hides the HUD."
				)
			) { _ ->
				cfg.enabled = !cfg.enabled
				if (!cfg.enabled) {
					kcl.spotfilter.client.world.PinnedSpotMarker.removeAll()
				}
				SpotFilterConfig.save()
				rebuildWidgets()
			}
		)

		val slotWidth = ((width - 24) / 3).coerceAtLeast(90)
		repeat(3) { index ->
			addRenderableWidget(
				Button.builder(slotLabel(index)) { _ ->
					minecraft.gui.setScreen(FilterSlotScreen(this, FilterState.slots[index], "Filter F${index + 1}"))
				}.tooltip(
					tip(
						"Perk filter slot F${index + 1}.",
						"Pick a perk, optional numeric compare, and sort direction.",
						"Sort uses F1 then F2 then F3. Click to configure (do not cycle by spam-clicking)."
					)
				).bounds(8 + index * (slotWidth + 4), 32, slotWidth, 20).build()
			)
		}
		val grotto = FilterState.kind == SpotKind.GROTTO
		if (grotto) {
			val third = ((width - 24) / 3).coerceAtLeast(80)
			addRenderableWidget(
				Button.builder(Component.literal(FilterState.stock.compactLabel())) { _ ->
					minecraft.gui.setScreen(StockFilterScreen(this, FilterState.stock))
				}.tooltip(
					tip(
						"Stock filter for the current mode.",
						"Depleted stays hidden unless this is On and the compare includes Depleted.",
						"Does not use an F1–F3 slot."
					)
				).bounds(8, 56, third, 20).build()
			)
			addRenderableWidget(
				Button.builder(Component.literal(FilterState.stability.compactLabel())) { _ ->
					minecraft.gui.setScreen(StabilityFilterScreen(this, FilterState.stability))
				}.tooltip(
					tip(
						"Grotto Stability Cost filter.",
						"Low is best (#65FEFE), then Medium, then High.",
						"Grotto pin color uses Cost unless Auto Pin sets a hex."
					)
				).bounds(12 + third, 56, third, 20).build()
			)
			addRenderableWidget(
				Button.builder(Component.literal("Auto Pin (${FilterState.autoPinRules.count { it.enabled }})")) { _ ->
					minecraft.gui.setScreen(ConfigPacksScreen(this))
				}.tooltip(
					tip(
						"Auto Pin config packs. Check several to run in parallel.",
						"Normal: <name>.txt   Grotto: <name>_grotto.txt",
						"Duplicate rule names share one numbering group."
					)
				).bounds(16 + third * 2, 56, third, 20).build()
			)
		} else {
			addRenderableWidget(
				Button.builder(Component.literal(FilterState.stock.compactLabel())) { _ ->
					minecraft.gui.setScreen(StockFilterScreen(this, FilterState.stock))
				}.tooltip(
					tip(
						"Stock filter for the current mode.",
						"Depleted stays hidden unless this is On and the compare includes Depleted.",
						"Does not use an F1–F3 slot."
					)
				).bounds(8, 56, (width - 20) / 2, 20).build()
			)
			addRenderableWidget(
				Button.builder(Component.literal("Auto Pin (${FilterState.autoPinRules.count { it.enabled }})")) { _ ->
					minecraft.gui.setScreen(ConfigPacksScreen(this))
				}.tooltip(
					tip(
						"Auto Pin config packs. Check several to run in parallel.",
						"Normal: <name>.txt   Grotto: <name>_grotto.txt",
						"Duplicate rule names share one numbering group."
					)
				).bounds(16 + (width - 20) / 2, 56, (width - 20) / 2, 20).build()
			)
		}
		addRenderableWidget(
			Button.builder(Component.literal(FilterState.pair.compactLabel())) { _ ->
				minecraft.gui.setScreen(PairFilterScreen(this, FilterState.pair))
			}.tooltip(
				tip(
					"Filter by perk1 + perk2 sum for this spot type.",
					"Fish: Strong + Wise Hook. Pearl/Treasure/Spirit: matching Hook + Magnet.",
					"Range +10% to +60%. Missing half counts as +0%. Separate for Normal / Grotto."
				)
			).bounds(8, 80, width - 16, 20).build()
		)
	}

	private class TopBtn(
		val message: Component,
		val tooltip: Tooltip,
		val onPress: (Button) -> Unit
	)

	private fun addTopRow(y: Int, vararg items: TopBtn) {
		val gap = 3
		val n = items.size
		val inner = width - 16 - gap * (n - 1)
		val base = (inner / n).coerceAtLeast(40)
		val extra = (inner - base * n).coerceAtLeast(0)
		var x = 8
		items.forEachIndexed { i, item ->
			val bw = base + if (i < extra) 1 else 0
			addRenderableWidget(
				Button.builder(item.message) { btn -> item.onPress(btn) }
					.tooltip(item.tooltip)
					.bounds(x, y, bw, 20)
					.build()
			)
			x += bw + gap
		}
	}

	private fun kindLabel(): Component =
		Component.literal(FilterState.kind.label)

	private fun modeLabel(): Component =
		Component.literal("Mode: ${FilterState.mode.name}")

	private fun layoutLabel(): Component =
		Component.literal(SpotFilterConfig.instance.layout().label)

	private fun enableLabel(): Component =
		Component.literal(if (SpotFilterConfig.instance.enabled) "Enabled" else "Disabled")

	private fun kickLabel(): Component =
		Component.literal(
			if (SpotFilterConfig.instance.kickDepleted) "Kick Depl: On" else "Kick Depl: Off"
		)

	private fun tip(vararg lines: String): Tooltip =
		Tooltip.create(CommonComponents.joinLines(lines.map { Component.literal(it) }))

	private fun slotLabel(index: Int): Component {
		val slot = FilterState.slots[index]
		return Component.literal("F${index + 1}: ${slot.compactLabel()}")
	}

	override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
		super.extractRenderState(graphics, mouseX, mouseY, delta)
		val font = this.font
		if (editingHud) {
			graphics.text(
				font,
				Component.literal(
					"Edit HUD: drag to move  |  scroll scale x${"%.1f".format(SpotFilterConfig.instance.hudScale)}  |  shift+scroll opacity ${SpotFilterConfig.instance.backgroundAlpha}%"
				),
				8,
				8,
				0xFFFFFFFF.toInt(),
				false
			)
			drawHudOutline(graphics)
			return
		}

		val spots = FilterState.filteredSorted()
		graphics.text(
			font,
			Component.literal("${spots.size} ${FilterState.kind.label.lowercase()} spots  |  click row to pin  |  F1>F2>F3 sort  |  O close"),
			8,
			106,
			0xFFCCCCCC.toInt(),
			false
		)

		val visibleRows = ((listBottom - listTop) / rowHeight).coerceAtLeast(1)
		if (scroll > (spots.size - visibleRows).coerceAtLeast(0)) {
			scroll = (spots.size - visibleRows).coerceAtLeast(0)
		}
		val start = scroll
		val end = (scroll + visibleRows).coerceAtMost(spots.size)
		var y = listTop
		for (i in start until end) {
			val spot = spots[i]
			drawRow(graphics, spot, 8, y, width - 16, mouseX, mouseY)
			y += rowHeight
		}
	}

	private fun drawHudOutline(graphics: GuiGraphicsExtractor) {
		val m = SpotHud.metrics()
		val x2 = m.x + m.screenWidth
		val y2 = m.y + m.screenHeight
		graphics.fill(m.x, m.y, x2, m.y + 1, 0xFFFFFFFF.toInt())
		graphics.fill(m.x, y2 - 1, x2, y2, 0xFFFFFFFF.toInt())
		graphics.fill(m.x, m.y, m.x + 1, y2, 0xFFFFFFFF.toInt())
		graphics.fill(x2 - 1, m.y, x2, y2, 0xFFFFFFFF.toInt())
	}

	private fun drawRow(
		graphics: GuiGraphicsExtractor,
		spot: kcl.spotfilter.client.data.FishingSpot,
		x: Int,
		y: Int,
		w: Int,
		mouseX: Int,
		mouseY: Int
	) {
		val hovered = mouseX in x until (x + w) && mouseY in y until (y + rowHeight - 2)
		val bg = when {
			spot.pinned && hovered -> 0x8866AA66.toInt()
			spot.pinned -> 0x55338855.toInt()
			hovered -> 0x55FFFFFF.toInt()
			else -> 0x33000000
		}
		graphics.fill(x, y, x + w, y + rowHeight - 2, bg)
		if (compact) {
			val parts = SpotLines.compactParts(spot).toMutableList()
			parts += LinePart(
				text = Component.literal(if (spot.pinned) "[PINNED]" else "[pin]").withStyle(
					Style.EMPTY.withColor(if (spot.pinned) 0x88FF88 else 0x888888)
				)
			)
			SpotLines.draw(graphics, font, parts, x + 4, y + 4)
			return
		}
		val primary = spot.primaryPerk()
		if (primary != null) {
			SpotLines.blitIcon(graphics, primary.type.textureId, x + 4, y + 6, 16)
		}
		val header = Component.literal("${spot.displayTitle()}  ${spot.x} ${spot.y} ${spot.z}  ")
			.append(
				Component.literal(spot.stock?.label ?: "?").withStyle(
					Style.EMPTY.withColor(spot.stockDisplayRgb())
				)
			)
		if (spot.kind == SpotKind.GROTTO) {
			spot.grottoChance()?.let { catch ->
				header.append(Component.literal("  "))
				header.append(
					Component.literal(catch.type.displayName.removeSuffix(" Chance")).withStyle(
						Style.EMPTY.withColor(catch.resolvedNameRgb())
					)
				)
			}
			if (!spot.stabilityRange.isNullOrBlank()) {
				header.append(Component.literal("  "))
				header.append(
					Component.literal(spot.stabilityRange!!).withStyle(
						Style.EMPTY.withColor(spot.stabilityDisplayRgb())
					)
				)
			}
		}
		header.append(Component.literal(if (spot.pinned) "  [PINNED]" else "  [pin]").withStyle(Style.EMPTY.withColor(if (spot.pinned) 0x88FF88 else 0x888888)))
		graphics.text(font, header, x + 26, y + 4, 0xFFFFFFFF.toInt(), false)
		var perkX = x + 26 + 8
		for (perk in spot.perks) {
			SpotLines.blitIcon(graphics, perk.type.textureId, perkX, y + 18, 10)
			perkX += 14
			graphics.text(font, perk.coloredLine(), perkX, y + 18, 0xFFFFFFFF.toInt(), false)
			perkX += font.width(perk.coloredLine()) + 10
		}
	}

	override fun mouseClicked(event: MouseButtonEvent, doubled: Boolean): Boolean {
		if (editingHud) {
			val cfg = SpotFilterConfig.instance
			val mx = event.x().toInt()
			val my = event.y().toInt()
			if (inHud(mx, my)) {
				dragging = true
				dragOffX = mx - cfg.hudX
				dragOffY = my - cfg.hudY
				return true
			}
		} else if (event.button() == 0) {
			val spots = FilterState.filteredSorted()
			val visibleRows = ((listBottom - listTop) / rowHeight).coerceAtLeast(1)
			val index = scroll + ((event.y().toInt() - listTop) / rowHeight)
			if (index in spots.indices && event.y().toInt() in listTop until listBottom && index < scroll + visibleRows) {
				val spot = spots[index]
				SpotPool.setPinned(spot, !spot.pinned)
				return true
			}
		}
		return super.mouseClicked(event, doubled)
	}

	override fun mouseReleased(event: MouseButtonEvent): Boolean {
		if (dragging) {
			dragging = false
			SpotFilterConfig.save()
			return true
		}
		return super.mouseReleased(event)
	}

	override fun mouseDragged(event: MouseButtonEvent, dx: Double, dy: Double): Boolean {
		val cfg = SpotFilterConfig.instance
		val mx = event.x().toInt()
		val my = event.y().toInt()
		if (dragging) {
			cfg.hudX = (mx - dragOffX).coerceAtLeast(0)
			cfg.hudY = (my - dragOffY).coerceAtLeast(0)
			return true
		}
		return super.mouseDragged(event, dx, dy)
	}

	override fun mouseScrolled(mouseX: Double, mouseY: Double, scrollX: Double, scrollY: Double): Boolean {
		if (editingHud) {
			val cfg = SpotFilterConfig.instance
			if (minecraft.hasShiftDown()) {
				cfg.backgroundAlpha = (cfg.backgroundAlpha + if (scrollY > 0) 5 else -5).coerceIn(0, 90)
			} else {
				val step = if (scrollY > 0) 0.1f else -0.1f
				cfg.hudScale = ((cfg.hudScale + step) * 10f).toInt() / 10f
				cfg.clamp()
			}
			return true
		}
		shiftList(-wheelRows(scrollY))
		return true
	}

	private fun shiftList(delta: Int) {
		if (delta == 0) return
		val spots = FilterState.filteredSorted()
		val visibleRows = ((listBottom - listTop) / rowHeight).coerceAtLeast(1)
		val max = (spots.size - visibleRows).coerceAtLeast(0)
		scroll = (scroll + delta).coerceIn(0, max)
	}

	override fun keyPressed(event: KeyEvent): Boolean {
		when (event.key()) {
			GLFW.GLFW_KEY_E -> {
				if (!editingHud) {
					editingHud = true
					rebuildWidgets()
					return true
				}
			}
			GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> {
				if (editingHud) {
					editingHud = false
					SpotFilterConfig.save()
					rebuildWidgets()
					return true
				}
			}
			GLFW.GLFW_KEY_ESCAPE -> {
				if (editingHud) {
					editingHud = false
					SpotFilterConfig.save()
					rebuildWidgets()
					return true
				}
			}
			GLFW.GLFW_KEY_O -> {
				if (typingInBox()) {
					return super.keyPressed(event) || true
				}
				onClose()
				return true
			}
			GLFW.GLFW_KEY_P -> {
				if (typingInBox()) {
					return super.keyPressed(event) || true
				}
				SpotPool.clearSpots()
				return true
			}
			GLFW.GLFW_KEY_UP -> {
				if (!editingHud) {
					shiftList(-1)
					return true
				}
			}
			GLFW.GLFW_KEY_DOWN -> {
				if (!editingHud) {
					shiftList(1)
					return true
				}
			}
			GLFW.GLFW_KEY_PAGE_UP -> {
				if (!editingHud) {
					val visibleRows = ((listBottom - listTop) / rowHeight).coerceAtLeast(1)
					shiftList(-visibleRows)
					return true
				}
			}
			GLFW.GLFW_KEY_PAGE_DOWN -> {
				if (!editingHud) {
					val visibleRows = ((listBottom - listTop) / rowHeight).coerceAtLeast(1)
					shiftList(visibleRows)
					return true
				}
			}
		}
		return super.keyPressed(event)
	}

	private fun inHud(mx: Int, my: Int): Boolean {
		val m = SpotHud.metrics()
		return mx in m.x until (m.x + m.screenWidth) && my in m.y until (m.y + m.screenHeight)
	}

}
