from pathlib import Path

ICONS = {
    "strong_hook": "\ue143",
    "wise_hook": "\ue148",
    "glimmering_hook": "\ue134",
    "greedy_hook": "\ue139",
    "lucky_hook": "\ue13c",
    "xp_magnet": "\ue14a",
    "fish_magnet": "\ue132",
    "pearl_magnet": "\ue13e",
    "treasure_magnet": "\ue146",
    "spirit_magnet": "\ue140",
    "wayfinder_data": "\ue103",
    "elusive_chance": "\ue10d",
    "pearl_chance": "\ue104",
    "treasure_chance": "\ue107",
    "spirit_chance": "\ue10a",
}

COLORS = {
    "strong_hook": "#FC5454",
    "wise_hook": "#2199F0",
    "glimmering_hook": "#8636FF",
    "greedy_hook": "#FC7D3F",
    "lucky_hook": "#23C525",
    "xp_magnet": "#FC5454",
    "fish_magnet": "#2199F0",
    "pearl_magnet": "#8636FF",
    "treasure_magnet": "#FC7D3F",
    "spirit_magnet": "#23C525",
    "elusive_chance": "#FC5454",
    "wayfinder_data": "#2199F0",
    "pearl_chance": "#8636FF",
    "treasure_chance": "#FC7D3F",
    "spirit_chance": "#23C525",
}

NAMES = {
    "strong_hook": "Strong Hook",
    "wise_hook": "Wise Hook",
    "glimmering_hook": "Glimmering Hook",
    "greedy_hook": "Greedy Hook",
    "lucky_hook": "Lucky Hook",
    "xp_magnet": "XP Magnet",
    "fish_magnet": "Fish Magnet",
    "pearl_magnet": "Pearl Magnet",
    "treasure_magnet": "Treasure Magnet",
    "spirit_magnet": "Spirit Magnet",
    "elusive_chance": "Elusive Chance",
    "wayfinder_data": "Wayfinder Data",
    "pearl_chance": "Pearl Chance",
    "treasure_chance": "Treasure Chance",
    "spirit_chance": "Spirit Chance",
}

STOCK_COLOR = {
    "Plentiful": "#A770FE",
    "Very High": "#55FFFF",
    "High": "#55FF55",
    "Medium": "#FFD83D",
    "Low": "#FF8C1A",
}

FIXED = {
    "elusive_chance": (5, True),
    "wayfinder_data": (10, False),
    "pearl_chance": (5, True),
    "treasure_chance": (1, True),
    "spirit_chance": (2, True),
}


def value_label(key: str, value: int) -> str:
    if key == "wayfinder_data":
        return f"+{value}"
    return f"+{value}%"


def perk_components(key: str, value: int | None = None) -> list[str]:
    if key in FIXED:
        value, _ = FIXED[key]
    assert value is not None
    icon = ICONS[key]
    color = COLORS[key]
    name = NAMES[key]
    label = value_label(key, value)
    return [
        f'{{text:"{icon}",font:"mcc:icon",bold:false,color:"white"}}',
        f'{{text:" {label} ",bold:false,color:"white"}}',
        f'{{text:"{name}",bold:false,color:"{color}"}}',
    ]


def summon(dx: int, dz: int, stock: str, perks: list[tuple[str, int | None]]) -> str:
    extras = [
        '{text:"\\n\\nStock: ",bold:false,color:"gray"}',
        f'{{text:"{stock}",bold:false,color:"{STOCK_COLOR[stock]}"}}',
        '{text:"\\n\\n",bold:false}',
    ]
    for i, perk in enumerate(perks):
        extras.extend(perk_components(perk[0], perk[1]))
        if i != len(perks) - 1:
            extras.append('{text:"\\n",bold:false}')
    extra = ",".join(extras)
    nbt = (
        '{Tags:["spotfilter_test"],billboard:"center",alignment:"center",shadow:1b,'
        'text:{text:"Fishing Spot",bold:true,color:"yellow",extra:[' + extra + "]}}"
    )
    return f"summon text_display ~{dx} ~1.2 ~{dz} {nbt}"


def spots() -> list[tuple[str, list[tuple[str, int | None]]]]:
    stocks = ["Plentiful", "Very High", "High", "Medium", "Low"]
    hooks = ["strong_hook", "wise_hook", "glimmering_hook", "greedy_hook", "lucky_hook"]
    magnets = ["xp_magnet", "fish_magnet", "pearl_magnet", "treasure_magnet", "spirit_magnet"]
    specials = ["elusive_chance", "wayfinder_data", "pearl_chance", "treasure_chance", "spirit_chance"]
    out: list[tuple[str, list[tuple[str, int | None]]]] = []

    # 1-15: single hooks 10/20/30
    for i, hook in enumerate(hooks):
        for j, pct in enumerate((10, 20, 30)):
            out.append((stocks[(i + j) % 5], [(hook, pct)]))

    # 16-30: single magnets 10/20/30
    for i, mag in enumerate(magnets):
        for j, pct in enumerate((10, 20, 30)):
            out.append((stocks[(i + j + 1) % 5], [(mag, pct)]))

    # 31-35: each special alone
    for i, spec in enumerate(specials):
        out.append((stocks[i], [(spec, None)]))

    # 36-40: dual Strong+Wise (only legal dual hooks)
    out.append(("Plentiful", [("strong_hook", 30), ("wise_hook", 30)]))
    out.append(("Very High", [("strong_hook", 30), ("wise_hook", 20)]))
    out.append(("High", [("strong_hook", 20), ("wise_hook", 30)]))
    out.append(("Medium", [("strong_hook", 10), ("wise_hook", 10)]))
    out.append(("Low", [("strong_hook", 30), ("wise_hook", 10)]))

    # 41-45: hook + magnet same family
    out.append(("Plentiful", [("strong_hook", 30), ("xp_magnet", 20)]))
    out.append(("Very High", [("wise_hook", 20), ("fish_magnet", 30)]))
    out.append(("High", [("glimmering_hook", 30), ("pearl_magnet", 10)]))
    out.append(("Medium", [("greedy_hook", 20), ("treasure_magnet", 30)]))
    out.append(("Low", [("lucky_hook", 10), ("spirit_magnet", 20)]))

    # 46-50: triples (max 3, mutex respected)
    out.append(("Plentiful", [("strong_hook", 30), ("wise_hook", 30), ("wayfinder_data", None)]))
    out.append(("Very High", [("glimmering_hook", 30), ("pearl_magnet", 20), ("pearl_chance", None)]))
    out.append(("High", [("greedy_hook", 20), ("treasure_magnet", 30), ("treasure_chance", None)]))
    out.append(("Medium", [("lucky_hook", 30), ("spirit_magnet", 10), ("spirit_chance", None)]))
    out.append(("Low", [("strong_hook", 30), ("xp_magnet", 20), ("elusive_chance", None)]))

    assert len(out) == 50, len(out)
    return out


def write_pack(root: Path) -> None:
    func = root / "data" / "spotfilter" / "function"
    func.mkdir(parents=True, exist_ok=True)
    (root / "pack.mcmeta").write_text(
        """{
  "pack": {
    "description": "SpotFilter test: 50 fishing spots",
    "min_format": [107, 1],
    "max_format": 107
  }
}
""",
        encoding="utf-8",
    )
    lines = [
        "kill @e[type=minecraft:text_display,tag=spotfilter_test]",
        'tellraw @s {"text":"Spawning 50 SpotFilter test spots (10x5 grid, 3 blocks apart)","color":"green"}',
    ]
    for i, (stock, perks) in enumerate(spots()):
        dx = (i % 10) * 3
        dz = (i // 10) * 3
        lines.append(summon(dx, dz, stock, perks))
    (func / "spawn.mcfunction").write_text("\n".join(lines) + "\n", encoding="utf-8")
    (func / "clear.mcfunction").write_text(
        "kill @e[type=minecraft:text_display,tag=spotfilter_test]\n"
        'tellraw @s {"text":"Cleared SpotFilter test spots","color":"yellow"}\n',
        encoding="utf-8",
    )
    (root / "README.txt").write_text(
        "SpotFilter test datapack\n"
        "\n"
        "In-game (cheats on):\n"
        "  /function spotfilter:spawn\n"
        "  /function spotfilter:clear\n"
        "\n"
        "50 text_display fishing spots in a 10x5 grid, 3 blocks apart, starting at your feet.\n"
        "Enable the MCCI resource pack so perk icons render.\n",
        encoding="utf-8",
    )


def main() -> None:
    here = Path(__file__).resolve().parent / "spotfilter-test"
    write_pack(here)
    world = Path(__file__).resolve().parent.parent / "run" / "saves" / "New World" / "datapacks" / "spotfilter-test"
    try:
        write_pack(world)
        print("wrote", here)
        print("wrote", world)
    except OSError as exc:
        print("wrote", here)
        print("world copy failed:", exc)


if __name__ == "__main__":
    main()
