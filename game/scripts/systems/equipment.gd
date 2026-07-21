class_name Equipment
extends RefCounted
## Lógica pura de equipamento (§3.3). Agrega os bônus das peças equipadas
## (armadura + amuleto) e resolve um id em EquipmentData. Sem estado/UI — testável.

const MAX_REDUCTION := 0.9   ## teto de redução de dano (nunca invulnerável)
const PATHS := [
	"res://data/equipment/couraca_de_sal.tres",
	"res://data/equipment/amuleto_do_eco.tres",
]

## Resolve um id de equipamento no seu recurso. Null se desconhecido.
static func by_id(id: StringName) -> EquipmentData:
	var target := String(id)
	if target == "":
		return null
	for p in PATHS:
		var e := load(p) as EquipmentData
		if e and String(e.id) == target:
			return e
	return null

## Soma os bônus das peças equipadas. Redução de dano com teto (§3.3).
static func aggregate(items: Array) -> Dictionary:
	var dr := 0.0
	var hp := 0.0
	var sr := 0.0
	for e in items:
		if e == null:
			continue
		dr += e.damage_reduction
		hp += e.max_hp_bonus
		sr += e.stamina_regen_bonus
	return {
		"damage_reduction": clampf(dr, 0.0, MAX_REDUCTION),
		"max_hp_bonus": hp,
		"stamina_regen_bonus": sr,
	}
