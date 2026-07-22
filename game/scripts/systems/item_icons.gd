class_name ItemIcons
extends RefCounted
## Cores de "ícone" greybox para itens de bolsa (§3.5). Enquanto não há arte, cada
## item ganha uma cor estável: os conhecidos têm cor curada e o resto deriva do
## hash do id (determinístico). Lógica pura — a grade do inventário desenha a partir
## daqui.

const COLORS := {
	"pocao_cura": Color(0.85, 0.3, 0.3),
	"pocao_vigor": Color(0.3, 0.75, 0.4),
	"elixir_do_eco": Color(0.6, 0.4, 0.9),
	"refeicao": Color(0.9, 0.7, 0.35),
	"erva": Color(0.4, 0.8, 0.5),
	"erva_prateada": Color(0.72, 0.8, 0.85),
	"erva_do_eco": Color(0.5, 0.85, 0.75),
	"madeira": Color(0.55, 0.4, 0.25),
	"cogumelo": Color(0.8, 0.55, 0.5),
	"peixe": Color(0.5, 0.65, 0.8),
	"inseto": Color(0.72, 0.75, 0.35),
	"minerio": Color(0.6, 0.6, 0.65),
	"minerio_ressonante": Color(0.5, 0.7, 0.9),
	"minerio_do_eco": Color(0.75, 0.55, 0.9),
}

## Cor do ícone do item. Conhecido: cor curada. Desconhecido: cor do hash, clareada
## para nunca ficar escura demais (mantém o greybox legível).
static func color_for(id: StringName) -> Color:
	var key := String(id)
	if COLORS.has(key):
		return COLORS[key]
	var h := abs(key.hash())
	var r := float((h >> 16) & 0xFF) / 255.0
	var g := float((h >> 8) & 0xFF) / 255.0
	var b := float(h & 0xFF) / 255.0
	return Color(0.35 + r * 0.55, 0.35 + g * 0.55, 0.35 + b * 0.55)

## Rótulo curto (primeira palavra) para caber na célula da grade.
static func short_label(id: StringName) -> String:
	var full := ItemNames.label(id)
	var space := full.find(" ")
	return full.substr(0, space) if space > 0 else full

## Lista ordenada de {id, count} de uma bolsa, ignorando quantidades zeradas.
static func cells_from(bag: Dictionary) -> Array:
	var cells: Array = []
	for key in bag:
		var n := int(bag[key])
		if n > 0:
			cells.append({"id": StringName(key), "count": n})
	return cells
