class_name NewGamePlus
extends RefCounted
## New Game+ (§3.6): rejogar mais difícil, mantendo a progressão. Barato de
## produzir (remix por multiplicadores), dobra a longevidade. Lógica pura.

## Multiplicadores de inimigo por ciclo (0 = primeira run, sem alteração).
static func health_mult(cycle: int) -> float:
	return 1.0 + 0.5 * maxi(cycle, 0)

static func damage_mult(cycle: int) -> float:
	return 1.0 + 0.3 * maxi(cycle, 0)

## Reinicia a RUN mantendo a progressão do jogador (atributos, armas, itens,
## melodias, memórias, recursos, Ecos) e zerando o estado do mundo (bosses,
## paredes, drop, mapa, quests). Muta e retorna o `state`.
static func reset_run(state: Dictionary) -> Dictionary:
	state["ng_cycle"] = int(state.get("ng_cycle", 0)) + 1
	var world: Dictionary = state.get("world", {})
	world["bosses_defeated"] = []
	world["walls_broken"] = []
	world["eco_drop"] = {}
	world["map"] = {}
	state["quests"] = {}
	return state
