package kcl.spotfilter.client.command

import com.mojang.brigadier.arguments.FloatArgumentType
import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import kcl.spotfilter.client.config.SpotFilterConfig
import kcl.spotfilter.client.data.SpotKind
import kcl.spotfilter.client.data.SpotPool
import kcl.spotfilter.client.filter.AutoPin
import kcl.spotfilter.client.filter.FilterMode
import kcl.spotfilter.client.filter.FilterState
import kcl.spotfilter.client.ui.FilterScreen
import kcl.spotfilter.client.world.PinnedSpotMarker
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.command.v2.ClientCommands.argument
import net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import java.util.concurrent.CompletableFuture

object SpotCommands {
	private const val OK = 0x88FF88
	private const val ERR = 0xFF5555
	private const val INFO = 0xAAAAAA
	private const val ACCENT = 0x55FFFF

	fun register() {
		ClientCommandRegistrationCallback.EVENT.register { dispatcher, _ ->
			val root = literal("sf")
				.executes { help(it) }
				.then(literal("help").executes { help(it) })
				.then(literal("status").executes { status(it) })
				.then(literal("on").executes { setEnabled(it, true) })
				.then(literal("off").executes { setEnabled(it, false) })
				.then(literal("toggle").executes { setEnabled(it, !SpotFilterConfig.instance.enabled) })
				.then(
					literal("hud")
						.executes { setHud(it, !SpotFilterConfig.instance.hudVisible) }
						.then(literal("on").executes { setHud(it, true) })
						.then(literal("off").executes { setHud(it, false) })
						.then(literal("toggle").executes { setHud(it, !SpotFilterConfig.instance.hudVisible) })
						.then(
							literal("scale").then(
								argument("value", FloatArgumentType.floatArg(SpotFilterConfig.MIN_SCALE, SpotFilterConfig.MAX_SCALE))
									.executes { setHudScale(it) }
							)
						)
						.then(
							literal("opacity").then(
								argument("percent", IntegerArgumentType.integer(0, 90))
									.executes { setHudOpacity(it) }
							)
						)
						.then(
							literal("pos").then(
								argument("x", IntegerArgumentType.integer(0))
									.then(
										argument("y", IntegerArgumentType.integer(0))
											.executes { setHudPos(it) }
									)
							)
						)
				)
				.then(
					literal("kind")
						.executes { ok(it, "Kind: ${FilterState.kind.label}") }
						.then(
							argument("value", StringArgumentType.word())
								.suggests { _, builder -> suggest(builder, "normal", "grotto", "toggle") }
								.executes { setKind(it) }
						)
				)
				.then(
					literal("logic")
						.executes { ok(it, "Filter logic: ${FilterState.mode.name}") }
						.then(
							argument("value", StringArgumentType.word())
								.suggests { _, builder -> suggest(builder, "and", "or", "toggle") }
								.executes { setLogic(it) }
						)
				)
				.then(literal("gui").executes { openGui(it) })
				.then(literal("filter").executes { openGui(it) })
				.then(literal("clear").executes { clear(it) })
				.then(literal("refresh").executes { clear(it) })
				.then(
					literal("list")
						.executes { listSpots(it, false) }
						.then(literal("pinned").executes { listSpots(it, true) })
						.then(literal("all").executes { listSpots(it, false) })
				)
				.then(
					literal("pin")
						.then(literal("all").executes { pinFiltered(it, true) })
						.then(
							argument("id", IntegerArgumentType.integer(1))
								.suggests { _, builder -> suggestIds(builder, pinned = false) }
								.executes { pinOne(it, true) }
						)
				)
				.then(
					literal("unpin")
						.then(literal("all").executes { pinFiltered(it, false) })
						.then(
							argument("id", IntegerArgumentType.integer(1))
								.suggests { _, builder -> suggestIds(builder, pinned = true) }
								.executes { pinOne(it, false) }
						)
				)
				.then(
					literal("autopin")
						.then(literal("apply").executes { applyAutoPin(it) })
				)
				.then(literal("reload").executes { reload(it) })
				.then(literal("save").executes { save(it) })

			dispatcher.register(root)
			dispatcher.register(literal("spotfilter").redirect(dispatcher.root.getChild("sf")))
		}
	}

	private fun help(ctx: CommandContext<FabricClientCommandSource>): Int {
		val lines = listOf(
			"/sf status — overlay, kind, HUD, spot counts",
			"/sf on | off | toggle — master overlay switch",
			"/sf hud [on|off|toggle] | scale <0.5-3> | opacity <0-90> | pos <x> <y>",
			"/sf kind <normal|grotto|toggle>",
			"/sf logic <and|or|toggle>",
			"/sf gui — open Filter screen",
			"/sf clear — clear scanned spots",
			"/sf list [pinned]",
			"/sf pin <id|all> | /sf unpin <id|all>",
			"/sf autopin apply",
			"/sf reload | /sf save"
		)
		info(ctx, "SpotFilter commands")
		lines.forEach { info(ctx, it) }
		return status(ctx)
	}

	private fun status(ctx: CommandContext<FabricClientCommandSource>): Int {
		val cfg = SpotFilterConfig.instance
		val kind = FilterState.kind
		val all = SpotPool.all()
		val kindCount = all.count { it.kind == kind }
		val pinned = SpotPool.pinned().size
		ok(
			ctx,
			"SpotFilter  ${if (cfg.enabled) "Enabled" else "Disabled"}  |  ${kind.label}  |  HUD ${if (cfg.hudVisible) "on" else "off"} x${"%.1f".format(cfg.hudScale)}  |  ${kindCount} ${kind.label.lowercase()} / ${all.size} spots  |  $pinned pinned  |  logic ${FilterState.mode.name}"
		)
		return 1
	}

	private fun setEnabled(ctx: CommandContext<FabricClientCommandSource>, enabled: Boolean): Int {
		val cfg = SpotFilterConfig.instance
		cfg.enabled = enabled
		if (!enabled) {
			PinnedSpotMarker.removeAll()
		}
		SpotFilterConfig.save()
		return ok(ctx, if (enabled) "SpotFilter enabled" else "SpotFilter disabled")
	}

	private fun setHud(ctx: CommandContext<FabricClientCommandSource>, visible: Boolean): Int {
		SpotFilterConfig.instance.hudVisible = visible
		SpotFilterConfig.save()
		return ok(ctx, if (visible) "HUD shown" else "HUD hidden")
	}

	private fun setHudScale(ctx: CommandContext<FabricClientCommandSource>): Int {
		val cfg = SpotFilterConfig.instance
		cfg.hudScale = FloatArgumentType.getFloat(ctx, "value")
		cfg.clamp()
		SpotFilterConfig.save()
		return ok(ctx, "HUD scale x${"%.1f".format(cfg.hudScale)}")
	}

	private fun setHudOpacity(ctx: CommandContext<FabricClientCommandSource>): Int {
		val cfg = SpotFilterConfig.instance
		cfg.backgroundAlpha = IntegerArgumentType.getInteger(ctx, "percent")
		cfg.clamp()
		SpotFilterConfig.save()
		return ok(ctx, "HUD opacity ${cfg.backgroundAlpha}%")
	}

	private fun setHudPos(ctx: CommandContext<FabricClientCommandSource>): Int {
		val cfg = SpotFilterConfig.instance
		cfg.hudX = IntegerArgumentType.getInteger(ctx, "x")
		cfg.hudY = IntegerArgumentType.getInteger(ctx, "y")
		cfg.clamp()
		SpotFilterConfig.save()
		return ok(ctx, "HUD position ${cfg.hudX}, ${cfg.hudY}")
	}

	private fun setKind(ctx: CommandContext<FabricClientCommandSource>): Int {
		val raw = StringArgumentType.getString(ctx, "value").lowercase()
		FilterState.kind = when (raw) {
			"normal" -> SpotKind.NORMAL
			"grotto" -> SpotKind.GROTTO
			"toggle" -> if (FilterState.kind == SpotKind.GROTTO) SpotKind.NORMAL else SpotKind.GROTTO
			else -> return err(ctx, "Kind must be normal, grotto, or toggle")
		}
		SpotFilterConfig.save()
		return ok(ctx, "Kind: ${FilterState.kind.label}")
	}

	private fun setLogic(ctx: CommandContext<FabricClientCommandSource>): Int {
		val raw = StringArgumentType.getString(ctx, "value").lowercase()
		FilterState.mode = when (raw) {
			"and" -> FilterMode.AND
			"or" -> FilterMode.OR
			"toggle" -> if (FilterState.mode == FilterMode.AND) FilterMode.OR else FilterMode.AND
			else -> return err(ctx, "Logic must be and, or, or toggle")
		}
		SpotFilterConfig.save()
		return ok(ctx, "Filter logic: ${FilterState.mode.name}")
	}

	private fun openGui(ctx: CommandContext<FabricClientCommandSource>): Int {
		val client = Minecraft.getInstance()
		client.gui.setScreen(FilterScreen())
		return ok(ctx, "Opened Filter")
	}

	private fun clear(ctx: CommandContext<FabricClientCommandSource>): Int {
		SpotPool.clearSpots()
		return ok(ctx, "Cleared fishing spots")
	}

	private fun listSpots(ctx: CommandContext<FabricClientCommandSource>, pinnedOnly: Boolean): Int {
		val spots = if (pinnedOnly) SpotPool.pinned() else FilterState.filteredSorted()
		if (spots.isEmpty()) {
			return ok(ctx, if (pinnedOnly) "No pinned spots" else "No matching ${FilterState.kind.label.lowercase()} spots")
		}
		info(ctx, "${spots.size} ${FilterState.kind.label.lowercase()} spot(s)${if (pinnedOnly) " pinned" else ""}:")
		spots.take(20).forEach { spot ->
			val pin = if (spot.pinned) "PIN" else "—"
			val extra = spot.grottoChance()?.type?.displayName?.removeSuffix(" Chance") ?: spot.stock?.label ?: "?"
			info(ctx, "#${spot.id}  ${spot.x} ${spot.y} ${spot.z}  $extra  [$pin]")
		}
		if (spots.size > 20) {
			info(ctx, "… ${spots.size - 20} more")
		}
		return 1
	}

	private fun pinOne(ctx: CommandContext<FabricClientCommandSource>, pin: Boolean): Int {
		val id = IntegerArgumentType.getInteger(ctx, "id")
		val spot = SpotPool.byId(id) ?: return err(ctx, "No spot #$id")
		if (spot.kind != FilterState.kind) {
			return err(ctx, "#$id is ${spot.kind.label}; switch with /sf kind ${spot.kind.label.lowercase()}")
		}
		SpotPool.setPinned(spot, pin)
		return ok(ctx, if (pin) "Pinned #${spot.id}" else "Unpinned #${spot.id}")
	}

	private fun pinFiltered(ctx: CommandContext<FabricClientCommandSource>, pin: Boolean): Int {
		val spots = FilterState.filteredSorted()
		var n = 0
		for (spot in spots) {
			if (spot.pinned != pin) {
				SpotPool.setPinned(spot, pin)
				n++
			}
		}
		return ok(ctx, if (pin) "Pinned $n spot(s)" else "Unpinned $n spot(s)")
	}

	private fun applyAutoPin(ctx: CommandContext<FabricClientCommandSource>): Int {
		AutoPin.applyAll()
		return ok(ctx, "Auto Pin reapplied (${FilterState.autoPinRules.count { it.enabled }} rules)")
	}

	private fun reload(ctx: CommandContext<FabricClientCommandSource>): Int {
		SpotFilterConfig.reload()
		return ok(ctx, "Reloaded config/spotfilter.json")
	}

	private fun save(ctx: CommandContext<FabricClientCommandSource>): Int {
		SpotFilterConfig.save()
		return ok(ctx, "Saved config/spotfilter.json")
	}

	private fun suggest(builder: SuggestionsBuilder, vararg values: String): CompletableFuture<Suggestions> {
		val remaining = builder.remaining.lowercase()
		values.filter { it.startsWith(remaining) }.forEach { builder.suggest(it) }
		return builder.buildFuture()
	}

	private fun suggestIds(builder: SuggestionsBuilder, pinned: Boolean): CompletableFuture<Suggestions> {
		SpotPool.all()
			.filter { it.kind == FilterState.kind && it.pinned == pinned }
			.forEach { builder.suggest(it.id.toString()) }
		return builder.buildFuture()
	}

	private fun ok(ctx: CommandContext<FabricClientCommandSource>, text: String): Int {
		ctx.source.sendFeedback(colored(text, OK))
		return 1
	}

	private fun err(ctx: CommandContext<FabricClientCommandSource>, text: String): Int {
		ctx.source.sendError(colored(text, ERR))
		return 0
	}

	private fun info(ctx: CommandContext<FabricClientCommandSource>, text: String) {
		ctx.source.sendFeedback(colored(text, if (text.startsWith("/")) ACCENT else INFO))
	}

	private fun colored(text: String, rgb: Int): Component =
		Component.literal(text).withStyle(Style.EMPTY.withColor(rgb))
}
