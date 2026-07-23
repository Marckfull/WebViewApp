class_name Alchemy
extends RefCounted
## Lógica pura da bancada da alquimista (§3.5): transforma ervas e itens de
## forrageio (madeira, cogumelo, peixe, inseto) em poções e refeições. Sem UI.
## O save guarda os insumos em state["resources"] (dicionário id -> quantidade).

## Receita legada (mantida por compatibilidade): 2 ervas comuns -> 1 Poção de Vigor.
const ERVAS_POR_POCAO := 2

## Bancada data-driven (§3.5). Cada receita: rótulo, saída (bolsa + id) e insumos.
## As ervas têm três tiers de potência; o forrageio alimenta receitas avançadas.
const RECIPES: Array = [
	{
		"label": "Poção de Vigor",
		"output": &"pocao_vigor",
		"output_bag": "consumables",
		"inputs": {&"erva": 2},
	},
	{
		"label": "Poção de Cura",
		"output": &"pocao_cura",
		"output_bag": "consumables",
		"inputs": {&"erva_prateada": 2},
	},
	{
		"label": "Elixir do Eco",
		"output": &"elixir_do_eco",
		"output_bag": "consumables",
		"inputs": {&"erva_do_eco": 1, &"cogumelo": 1},
	},
	{
		"label": "Refeição Farta",
		"output": &"refeicao",
		"output_bag": "consumables",
		"inputs": {&"madeira": 1, &"peixe": 1, &"inseto": 1},
	},
]

static func can_brew(ervas: int) -> bool:
	return ervas >= ERVAS_POR_POCAO

## Quantas poções dá para fazer com `ervas` ervas.
static func brewable(ervas: int) -> int:
	return ervas / ERVAS_POR_POCAO

## Verdadeiro se a bolsa `resources` tem todos os insumos da receita.
static func can_make(recipe: Dictionary, resources: Dictionary) -> bool:
	for id in recipe["inputs"]:
		if int(resources.get(String(id), 0)) < int(recipe["inputs"][id]):
			return false
	return true

## Consome os insumos da receita da bolsa (muta). Só chame após can_make().
static func spend_inputs(recipe: Dictionary, resources: Dictionary) -> void:
	for id in recipe["inputs"]:
		var key := String(id)
		resources[key] = int(resources.get(key, 0)) - int(recipe["inputs"][id])

## Texto "2x Erva, 1x Cogumelo" dos insumos, para a UI.
static func inputs_text(recipe: Dictionary) -> String:
	var parts: Array[String] = []
	for id in recipe["inputs"]:
		parts.append("%dx %s" % [int(recipe["inputs"][id]), ItemNames.label(id)])
	return ", ".join(parts)
