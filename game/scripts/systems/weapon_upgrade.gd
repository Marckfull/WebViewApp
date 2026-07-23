class_name WeaponUpgrade
extends RefCounted
## Lógica pura do upgrade de arma na forja da Mestra Odara (§3.3). Sem UI/estado.
## Cada nível custa mais Ecos e minério, e some ao dano da arma.

const MAX_LEVEL := 5

## Tiers de minério (§3.3/§3.5): níveis altos da forja exigem minério mais raro,
## achado em dungeons mais fundas. Progressão de recurso, não só de Ecos.
const TIER_BRUTO := &"minerio"
const TIER_RESSONANTE := &"minerio_ressonante"
const TIER_ECO := &"minerio_do_eco"

static func ecos_cost(level: int) -> int:
	return 50 + 40 * level

## Qual tier de minério é preciso para subir DE `level` para `level+1`.
static func required_tier(level: int) -> StringName:
	if level <= 1:
		return TIER_BRUTO       ## +1, +2
	elif level <= 3:
		return TIER_RESSONANTE  ## +3, +4
	return TIER_ECO             ## +5

## Nome amigável do tier (para a UI da forja).
static func tier_label(id: StringName) -> String:
	match id:
		TIER_BRUTO: return "minério bruto"
		TIER_RESSONANTE: return "minério ressonante"
		TIER_ECO: return "minério do eco"
		_: return String(id)

## Quantidade do tier exigido (tiers raros exigem menos unidades).
static func minerio_cost(level: int) -> int:
	return 1 if level >= 4 else 2

## Multiplicador de dano aplicado à arma pelo nível de upgrade.
static func damage_multiplier(level: int) -> float:
	return 1.0 + 0.2 * level

## `ore` = quantidade do tier exigido (required_tier) que Aria possui.
static func can_upgrade(level: int, ecos: int, ore: int) -> bool:
	if level >= MAX_LEVEL:
		return false
	return ecos >= ecos_cost(level) and ore >= minerio_cost(level)
