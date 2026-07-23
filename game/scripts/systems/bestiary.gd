class_name Bestiary
extends RefCounted
## Lógica pura do bestiário (§3.6) — preenchido automaticamente ao derrotar
## inimigos. Sem estado/UI; testável (§6.3).

## Registra um inimigo. Retorna true se foi a PRIMEIRA vez (entrada nova).
## Muta a lista recebida (é o registro vivo do save).
static func record(enemy_id: StringName, entries: Array) -> bool:
	var key := String(enemy_id)
	if key == "" or entries.has(key):
		return false
	entries.append(key)
	return true

static func has_entry(enemy_id: StringName, entries: Array) -> bool:
	return entries.has(String(enemy_id))

static func count(entries: Array) -> int:
	return entries.size()
