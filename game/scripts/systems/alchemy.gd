class_name Alchemy
extends RefCounted
## Lógica pura da alquimia (§3.5): converte ervas coletadas em poções. Sem UI.

const ERVAS_POR_POCAO := 2

static func can_brew(ervas: int) -> bool:
	return ervas >= ERVAS_POR_POCAO

## Quantas poções dá para fazer com `ervas` ervas.
static func brewable(ervas: int) -> int:
	return ervas / ERVAS_POR_POCAO
