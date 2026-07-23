extends "res://addons/gut/test.gd"
## Testa a política pura de streaming de salas (§6.2): o que pré-carregar e o que
## liberar do cache LRU.

func test_to_preload_skips_cached_and_blanks() -> void:
	var neighbors := ["res://a.tscn", "res://b.tscn", "", "res://a.tscn"]
	var cached := ["res://b.tscn"]
	var out := Streaming.to_preload(neighbors, cached)
	assert_eq(out, ["res://a.tscn"], "só o vizinho novo, sem repetir nem vazio")

func test_to_preload_empty_when_all_cached() -> void:
	assert_eq(Streaming.to_preload(["res://a.tscn"], ["res://a.tscn"]).size(), 0)

func test_evictions_none_within_cap() -> void:
	var lru := ["res://a.tscn", "res://b.tscn"]
	assert_eq(Streaming.evictions(lru, [], 4).size(), 0, "abaixo do teto: nada sai")

func test_evictions_drops_oldest_first() -> void:
	# 5 no cache, teto 3 → libera os 2 mais antigos.
	var lru := ["res://a.tscn", "res://b.tscn", "res://c.tscn", "res://d.tscn", "res://e.tscn"]
	var out := Streaming.evictions(lru, [], 3)
	assert_eq(out, ["res://a.tscn", "res://b.tscn"], "os mais antigos primeiro")

func test_evictions_never_touches_protected() -> void:
	var lru := ["res://a.tscn", "res://b.tscn", "res://c.tscn", "res://d.tscn", "res://e.tscn"]
	# 'a' e 'b' são a sala atual + vizinho: protegidos, mesmo sendo os mais antigos.
	var out := Streaming.evictions(lru, ["res://a.tscn", "res://b.tscn"], 3)
	assert_false(out.has("res://a.tscn"), "sala atual nunca sai")
	assert_false(out.has("res://b.tscn"), "vizinho nunca sai")
	assert_eq(out, ["res://c.tscn", "res://d.tscn"], "libera os próximos mais antigos")

func test_room_streamer_starts_empty() -> void:
	assert_false(RoomStreamer.is_cached("res://qualquer.tscn"))
	assert_null(RoomStreamer.take("res://nao_solicitado.tscn"), "sem request → null")
