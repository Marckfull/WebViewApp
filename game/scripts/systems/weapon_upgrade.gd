class_name WeaponUpgrade
extends RefCounted
## Lógica pura do upgrade de arma na forja da Mestra Odara (§3.3). Sem UI/estado.
## Cada nível custa mais Ecos e minério, e some ao dano da arma.

const MAX_LEVEL := 5

static func ecos_cost(level: int) -> int:
	return 50 + 40 * level

static func minerio_cost(level: int) -> int:
	return 1 + level

## Multiplicador de dano aplicado à arma pelo nível de upgrade.
static func damage_multiplier(level: int) -> float:
	return 1.0 + 0.2 * level

static func can_upgrade(level: int, ecos: int, minerio: int) -> bool:
	if level >= MAX_LEVEL:
		return false
	return ecos >= ecos_cost(level) and minerio >= minerio_cost(level)
