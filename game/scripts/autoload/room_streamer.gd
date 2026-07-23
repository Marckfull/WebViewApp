extends Node
## RoomStreamer — streaming leve de salas (autoload, §6.2). Ao entrar numa sala,
## pré-carrega em thread as salas vizinhas (alvos das portas) como "chunks" e as
## serve já prontas na transição, deixando a troca instantânea. Mantém um cache
## LRU com teto para não segurar o mundo todo. A política pura vem de Streaming.

const MAX_CACHE := 4

var _cached: Dictionary = {}     ## path(String) -> PackedScene pronta
var _requested: Dictionary = {}  ## path(String) -> true (thread em andamento)
var _lru: Array = []             ## paths, mais antigo → mais recente

## Chamado pela sala ao carregar: registra a atual e pré-carrega os vizinhos.
func enter(current_path: String, neighbors: Array) -> void:
	_touch(current_path)
	for n in Streaming.to_preload(neighbors, _cached.keys()):
		_request(n)
	_evict(current_path, neighbors)

## Transição de porta: usa a PackedScene já pronta (instantânea) ou cai no load
## por arquivo se ainda não veio. Mantém o contrato de next_spawn das portas.
func go_to(path: String, spawn: Vector2) -> void:
	GameConfig.next_spawn = spawn
	GameConfig.has_next_spawn = true
	var packed := take(path)
	if packed != null:
		get_tree().change_scene_to_packed(packed)
	else:
		get_tree().change_scene_to_file(path)

## Finaliza a thread (se houver) e devolve a PackedScene da sala, ou null.
func take(path: String) -> PackedScene:
	if _cached.has(path):
		return _cached[path]
	if _requested.has(path):
		var ps := ResourceLoader.load_threaded_get(path) as PackedScene
		_requested.erase(path)
		if ps != null:
			_cached[path] = ps
			_touch(path)
		return ps
	return null

func is_cached(path: String) -> bool:
	return _cached.has(path)

func _request(path: String) -> void:
	if path == "" or _cached.has(path) or _requested.has(path):
		return
	if ResourceLoader.load_threaded_request(path) == OK:
		_requested[path] = true

func _touch(path: String) -> void:
	if path == "":
		return
	_lru.erase(path)
	_lru.append(path)

func _evict(current_path: String, neighbors: Array) -> void:
	var protected: Array = [current_path]
	protected.append_array(neighbors)
	for path in Streaming.evictions(_lru, protected, MAX_CACHE):
		_lru.erase(path)
		_cached.erase(path)
		_requested.erase(path)
