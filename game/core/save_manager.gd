extends Node
## Salva/carrega o progresso em JSON (user://) com escrita atômica.
## Autosave em eventos-chave e quando o app fecha ou vai para segundo
## plano — essencial em mobile, onde o jogo é interrompido o tempo todo.

const SAVE_PATH := "user://save_v1.json"
const SAVE_VERSION := 1


func _ready() -> void:
	GameEvents.shrine_rested.connect(_on_shrine_rested)
	GameEvents.boss_ended.connect(_on_boss_ended)
	GameState.echoes_changed.connect(_on_echoes_changed)
	GameState.inventory_changed.connect(save_game)
	GameState.weapon_changed.connect(_on_weapon_changed)
	GameState.flasks_changed.connect(_on_flasks_changed)
	GameState.attributes_changed.connect(save_game)


func _notification(what: int) -> void:
	if what == NOTIFICATION_WM_CLOSE_REQUEST \
			or what == NOTIFICATION_APPLICATION_PAUSED:
		save_game()


func has_save() -> bool:
	return FileAccess.file_exists(SAVE_PATH)


## O save existente já zerou o jogo? (habilita o New Game+ no título.)
func save_is_completed() -> bool:
	if not has_save():
		return false
	var file := FileAccess.open(SAVE_PATH, FileAccess.READ)
	if file == null:
		return false
	var parsed: Variant = JSON.parse_string(file.get_as_text())
	if typeof(parsed) != TYPE_DICTIONARY:
		return false
	var saved_flags: Dictionary = parsed.get("flags", {})
	return bool(saved_flags.get("jogo_concluido", false))


func save_game() -> void:
	var data := {
		"version": SAVE_VERSION,
		"echoes": GameState.echoes,
		"flags": GameState.flags,
		"inventory": GameState.inventory,
		"weapon_level": GameState.weapon_level,
		"equipped_weapon": GameState.equipped_weapon,
		"flasks": GameState.flasks,
		"flasks_max": GameState.flasks_max,
		"attributes": GameState.attributes,
		"time_of_day": GameState.time_of_day,
		"map_data": GameState.map_data,
		"bestiary": GameState.bestiary,
		"shrine_scene": GameState.last_shrine_scene,
		"difficulty": GameState.difficulty,
		"ng_cycle": GameState.ng_cycle,
	}
	var tmp_path := SAVE_PATH + ".tmp"
	var file := FileAccess.open(tmp_path, FileAccess.WRITE)
	if file == null:
		return
	file.store_string(JSON.stringify(data))
	file.close()
	DirAccess.rename_absolute(tmp_path, SAVE_PATH)


func load_game() -> bool:
	if not has_save():
		return false
	var file := FileAccess.open(SAVE_PATH, FileAccess.READ)
	if file == null:
		return false
	var parsed: Variant = JSON.parse_string(file.get_as_text())
	if typeof(parsed) != TYPE_DICTIONARY:
		return false
	GameState.echoes = int(parsed.get("echoes", 0))
	GameState.flags = parsed.get("flags", {})
	var inv: Dictionary = parsed.get("inventory", {})
	for id in inv:
		inv[id] = int(inv[id])  # JSON devolve números como float
	GameState.inventory = inv
	GameState.weapon_level = int(parsed.get("weapon_level", 0))
	GameState.equipped_weapon = str(parsed.get("equipped_weapon", "espada"))
	GameState.flasks_max = int(parsed.get("flasks_max", 3))
	GameState.flasks = int(parsed.get("flasks", GameState.flasks_max))
	var attrs: Dictionary = parsed.get("attributes", {})
	for key in GameState.attributes:
		GameState.attributes[key] = int(attrs.get(key, 0))
	GameState.time_of_day = float(parsed.get("time_of_day", 0.15))
	GameState.difficulty = clampi(int(parsed.get("difficulty", 1)), 0, 2)
	GameState.ng_cycle = maxi(int(parsed.get("ng_cycle", 0)), 0)
	var raw_bestiary: Dictionary = parsed.get("bestiary", {})
	for id in raw_bestiary:
		raw_bestiary[id] = int(raw_bestiary[id])
	GameState.bestiary = raw_bestiary
	var raw_map: Dictionary = parsed.get("map_data", {})
	for scene in raw_map:
		var entry: Dictionary = raw_map[scene]
		entry["cols"] = int(entry.get("cols", 1))
		entry["rows"] = int(entry.get("rows", 1))
		var cells: Array = entry.get("cells", [])
		for i in cells.size():
			cells[i] = int(cells[i])  # JSON devolve números como float
	GameState.map_data = raw_map
	GameState.last_shrine_scene = str(
			parsed.get("shrine_scene", "res://world/village.tscn"))
	# A jogadora acorda no último santuário onde descansou (regra souls).
	GameState.next_spawn = "__shrine__"
	GameState.echoes_changed.emit(GameState.echoes)
	return true


func delete_save() -> void:
	if has_save():
		DirAccess.remove_absolute(SAVE_PATH)


func _on_shrine_rested() -> void:
	var scene := get_tree().current_scene
	if scene and not scene.scene_file_path.is_empty():
		GameState.last_shrine_scene = scene.scene_file_path
	save_game()


func _on_boss_ended(victory: bool) -> void:
	if victory:
		save_game()


func _on_echoes_changed(_amount: int) -> void:
	save_game()


func _on_weapon_changed(_level: int) -> void:
	save_game()


func _on_flasks_changed(_current: int, _max_value: int) -> void:
	save_game()
