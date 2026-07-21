extends Node
## TelemetryLogger — telemetria opt-in de playtest (autoload, §6.3).
##
## DESABILITADA por padrão. Quando ligada nas Opções, grava eventos (onde Aria
## morre, onde derrota bosses, onde descansa) num arquivo JSONL local em
## user://telemetry/ — dados para balancear a dificuldade depois. Nada sai do
## dispositivo; é só um registro local para o desenvolvedor/playtester.

const DIR := "user://telemetry"

var _file: FileAccess
var _playtime: float = 0.0

func _ready() -> void:
	set_process(true)
	GameEvents.player_died.connect(_on_player_died)
	GameEvents.boss_defeated.connect(_on_boss_defeated)
	GameEvents.rested_at_shrine.connect(_on_rested)

func _process(delta: float) -> void:
	if is_enabled():
		_playtime += delta

func is_enabled() -> bool:
	return bool(SaveManager.state.get("settings", {}).get("telemetry", false))

func set_enabled(on: bool) -> void:
	SaveManager.state["settings"]["telemetry"] = on
	if on:
		_open()
	else:
		_close()
	SaveManager.save_game()

func _open() -> void:
	if _file != null:
		return
	DirAccess.make_dir_recursive_absolute(DIR)
	var path := "%s/session_%d.jsonl" % [DIR, int(Time.get_unix_time_from_system())]
	_file = FileAccess.open(path, FileAccess.WRITE)

func _close() -> void:
	if _file:
		_file.close()
		_file = null

func _log(type: String, data: Dictionary) -> void:
	if not is_enabled():
		return
	if _file == null:
		_open()
	if _file:
		_file.store_line(Telemetry.to_line(Telemetry.make_event(type, data, _playtime)))
		_file.flush()

func _scene_name() -> String:
	var cs := get_tree().current_scene
	return cs.scene_file_path if cs else ""

func _on_player_died(pos: Vector2, ecos: int) -> void:
	_log("death", { "scene": _scene_name(), "x": pos.x, "y": pos.y, "ecos": ecos })

func _on_boss_defeated(boss_id: StringName) -> void:
	_log("boss_defeated", { "scene": _scene_name(), "boss": String(boss_id) })

func _on_rested(_shrine_id: StringName) -> void:
	_log("rest", { "scene": _scene_name() })
