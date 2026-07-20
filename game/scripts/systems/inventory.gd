class_name Inventory
extends RefCounted
## Lógica pura de coleções do save (itens-chave, memórias) — §3.3/§6.2. Sem UI.
## Usada para o gating metroidvania (ter o Gancho abre passagens) e as Memórias.

## Concede um id à coleção. Retorna true se foi novo (muta a lista).
static func grant(id: StringName, collection: Array) -> bool:
	var key := String(id)
	if key == "" or collection.has(key):
		return false
	collection.append(key)
	return true

static func has(id: StringName, collection: Array) -> bool:
	return collection.has(String(id))

static func count(collection: Array) -> int:
	return collection.size()
