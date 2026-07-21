class_name ActProgress
extends RefCounted
## Progresso do Ato 2 (§2): os 4 Santuários do Eco só se "restauram" ao derrotar
## os 4 bosses das dungeons. Lógica pura sobre a lista bosses_defeated do save —
## a descida ao Coração Mudo (Ato 3) só libera com os 4. Testável, sem estado.

const DUNGEON_BOSSES := [
	&"coro_enraizado",   ## Floresta Sussurrante (gancho)
	&"martelo_mudo",     ## Forja Afundada (bomba)
	&"sino_invertido",   ## Torre dos Ventos (lente)
	&"mare_salgada",     ## Necrópole de Sal (botas)
]

static func cleared_count(defeated: Array) -> int:
	var n := 0
	for b in DUNGEON_BOSSES:
		if defeated.has(String(b)):
			n += 1
	return n

static func all_cleared(defeated: Array) -> bool:
	return cleared_count(defeated) >= DUNGEON_BOSSES.size()

static func remaining(defeated: Array) -> int:
	return DUNGEON_BOSSES.size() - cleared_count(defeated)
