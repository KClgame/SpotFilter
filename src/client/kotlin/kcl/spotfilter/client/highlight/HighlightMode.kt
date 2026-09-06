package kcl.spotfilter.client.highlight

enum class HighlightMode(val label: String) {
	GLOWING("Glowing"),
	SOLO("Solo");

	fun toggle(): HighlightMode = if (this == GLOWING) SOLO else GLOWING

	companion object {
		fun fromName(raw: String?): HighlightMode =
			entries.firstOrNull { it.name.equals(raw?.trim(), ignoreCase = true) } ?: GLOWING
	}
}
