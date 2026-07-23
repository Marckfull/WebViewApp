class_name Consumables
extends RefCounted
## Lógica pura de consumíveis contáveis (§3.5) — poções e afins. Sem UI/estado.
## O save guarda um dicionário id -> quantidade.

static func grant(id: StringName, bag: Dictionary, amount: int = 1) -> void:
	var key := String(id)
	if key == "" or amount <= 0:
		return
	bag[key] = int(bag.get(key, 0)) + amount

## Consome 1. Retorna true se havia ao menos um.
static func consume(id: StringName, bag: Dictionary) -> bool:
	var key := String(id)
	var have := int(bag.get(key, 0))
	if have <= 0:
		return false
	bag[key] = have - 1
	return true

static func count(id: StringName, bag: Dictionary) -> int:
	return int(bag.get(String(id), 0))
