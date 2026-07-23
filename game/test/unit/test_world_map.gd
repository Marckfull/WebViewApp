extends "res://addons/gut/test.gd"
## Testa o mapa-múndi (§3.5): nomes/regiões e o cruzamento com o fog-of-war.

func test_known_room_name_and_region() -> void:
	assert_eq(WorldMap.name_of("res://scenes/world/floresta_bosque.tscn"), "Bosque")
	assert_eq(WorldMap.region_of("res://scenes/world/floresta_bosque.tscn"), "Floresta Sussurrante")

func test_unknown_room_is_blank() -> void:
	assert_eq(WorldMap.name_of("res://scenes/world/inexistente.tscn"), "")
	assert_eq(WorldMap.region_of("res://scenes/world/inexistente.tscn"), "")

func test_every_room_region_is_listed() -> void:
	# Toda sala aponta para uma região que existe em REGIONS (sem órfãs).
	for path in WorldMap.ROOMS:
		assert_true(WorldMap.REGIONS.has(WorldMap.ROOMS[path]["region"]),
			"%s tem região válida" % path)

func test_discovery_requires_revealed_cells() -> void:
	var atrio := "res://scenes/world/floresta_atrio.tscn"
	var bosque := "res://scenes/world/floresta_bosque.tscn"
	var world_state := {"map": {atrio: ["0,0", "1,0"]}}  # só o átrio tem células
	assert_true(WorldMap.is_discovered(atrio, world_state))
	assert_false(WorldMap.is_discovered(bosque, world_state), "sem células = não descoberto")
	# Célula vazia também conta como não descoberto.
	assert_false(WorldMap.is_discovered(atrio, {"map": {atrio: []}}))

func test_discovered_in_filters_region_and_visit() -> void:
	var atrio := "res://scenes/world/floresta_atrio.tscn"
	var world_state := {"map": {atrio: ["0,0"]}}
	var rooms := WorldMap.discovered_in("Floresta Sussurrante", world_state)
	assert_eq(rooms.size(), 1, "só o átrio foi visitado")
	assert_eq(rooms[0], atrio)
	assert_eq(WorldMap.discovered_in("Forja Afundada", world_state).size(), 0)

func test_counts() -> void:
	var atrio := "res://scenes/world/floresta_atrio.tscn"
	var forja := "res://scenes/world/forja_afundada.tscn"
	var world_state := {"map": {atrio: ["0,0"], forja: ["2,3"]}}
	assert_eq(WorldMap.discovered_count(world_state), 2)
	assert_eq(WorldMap.total_rooms(), WorldMap.ROOMS.size())
	assert_gt(WorldMap.total_rooms(), WorldMap.discovered_count(world_state))
