class_name Streaming
extends RefCounted
## Decisão pura do streaming de salas (§6.2). Cada sala vizinha (alvo de uma porta)
## é tratada como um "chunk" a pré-carregar em segundo plano; um cache LRU com teto
## evita segurar o mundo inteiro na memória. Sem IO — o RoomStreamer aplica.

## Vizinhos que ainda não estão no cache (a pré-carregar). Ignora vazios e repetidos.
static func to_preload(neighbors: Array, cached: Array) -> Array:
	var out: Array = []
	for n in neighbors:
		var key := String(n)
		if key != "" and not cached.has(key) and not out.has(key):
			out.append(key)
	return out

## Quais paths liberar do cache LRU (mais antigo → mais recente) para respeitar o
## teto `max_size`, nunca removendo os protegidos (sala atual + vizinhos).
static func evictions(lru: Array, protected: Array, max_size: int) -> Array:
	var evict: Array = []
	var remaining := lru.size()
	for path in lru:  ## do mais antigo para o mais recente
		if remaining <= max_size:
			break
		if protected.has(path):
			continue
		evict.append(path)
		remaining -= 1
	return evict
