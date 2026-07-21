class_name ConsumableSlots
extends RefCounted
## Acesso rápido a 4 slots de consumível no HUD (§3.5). Lógica pura de seleção.

## Atribuição dos 4 slots (id vazio = slot livre).
const DEFAULT: Array = [&"pocao_vigor", &"pocao_cura", &"", &""]

## Próximo slot NÃO vazio a partir de `current` (cicla). Se todos vazios, mantém.
static func next_slot(slots: Array, current: int) -> int:
	var n := slots.size()
	if n == 0:
		return current
	for i in range(1, n + 1):
		var idx := (current + i) % n
		if String(slots[idx]) != "":
			return idx
	return current

static func short_name(id: StringName) -> String:
	match id:
		&"pocao_vigor": return "Vigor"
		&"pocao_cura": return "Cura"
		_: return "—"
