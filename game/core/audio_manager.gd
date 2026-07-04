extends Node
## Toca SFX (pool de players) e música com loop. A música continua
## durante pausas (diálogo/inventário) e troca sozinha em lutas de boss.

const SFX_PLAYERS := 8
const SFX_DIR := "res://assets/audio/sfx/"
const MUSIC_DIR := "res://assets/audio/music/"
const SETTINGS_PATH := "user://settings.json"
const MUSIC_BASE_DB := -11.0
const SFX_BASE_DB := -4.0

## Volumes de 0 a 10 (menu de pause); persistidos em user://.
var music_volume := 8
var sfx_volume := 8

var _sfx_pool: Array[AudioStreamPlayer] = []
var _music_player: AudioStreamPlayer
var _level_music := ""
var _current_music := ""
var _cache: Dictionary = {}


func _ready() -> void:
	process_mode = Node.PROCESS_MODE_ALWAYS
	for i in SFX_PLAYERS:
		var p := AudioStreamPlayer.new()
		add_child(p)
		_sfx_pool.append(p)
	_music_player = AudioStreamPlayer.new()
	add_child(_music_player)
	_load_settings()
	_apply_volumes()
	GameEvents.player_died.connect(_on_player_died)
	GameEvents.shrine_rested.connect(play_sfx.bind("shrine"))
	GameEvents.boss_engaged.connect(_on_boss_engaged)
	GameEvents.boss_ended.connect(_on_boss_ended)


func set_music_volume(value: int) -> void:
	music_volume = clampi(value, 0, 10)
	_apply_volumes()
	_save_settings()


func set_sfx_volume(value: int) -> void:
	sfx_volume = clampi(value, 0, 10)
	_apply_volumes()
	_save_settings()


func _volume_db(volume: int, base: float) -> float:
	if volume <= 0:
		return -80.0
	return linear_to_db(volume / 10.0) + base


func _apply_volumes() -> void:
	_music_player.volume_db = _volume_db(music_volume, MUSIC_BASE_DB)
	for p in _sfx_pool:
		p.volume_db = _volume_db(sfx_volume, SFX_BASE_DB)


func _load_settings() -> void:
	if not FileAccess.file_exists(SETTINGS_PATH):
		return
	var file := FileAccess.open(SETTINGS_PATH, FileAccess.READ)
	if file == null:
		return
	var parsed: Variant = JSON.parse_string(file.get_as_text())
	if typeof(parsed) == TYPE_DICTIONARY:
		music_volume = clampi(int(parsed.get("music", 8)), 0, 10)
		sfx_volume = clampi(int(parsed.get("sfx", 8)), 0, 10)


func _save_settings() -> void:
	var file := FileAccess.open(SETTINGS_PATH, FileAccess.WRITE)
	if file == null:
		return
	file.store_string(JSON.stringify(
			{"music": music_volume, "sfx": sfx_volume}))
	file.close()


func play_sfx(name: String) -> void:
	var stream := _load_stream(SFX_DIR + name + ".wav")
	if stream == null:
		return
	for p in _sfx_pool:
		if not p.playing:
			p.stream = stream
			p.play()
			return
	_sfx_pool[0].stream = stream
	_sfx_pool[0].play()


## Música ambiente da cena atual (retomada quando um boss termina).
func play_level_music(name: String) -> void:
	_level_music = name
	play_music(name)


func play_music(name: String) -> void:
	if name == _current_music:
		return
	_current_music = name
	if name.is_empty():
		_music_player.stop()
		return
	var stream := _load_stream(MUSIC_DIR + name + ".wav")
	if stream == null:
		return
	if stream is AudioStreamWAV:
		stream.loop_mode = AudioStreamWAV.LOOP_FORWARD
		stream.loop_begin = 0
		stream.loop_end = stream.data.size() / 2  # frames (16-bit mono)
	_music_player.stream = stream
	_music_player.play()


func _load_stream(path: String) -> AudioStream:
	if not _cache.has(path):
		_cache[path] = load(path)
	return _cache[path]


func _on_player_died(_position: Vector2) -> void:
	play_sfx("death")


func _on_boss_engaged(_name: String, _max_health: int) -> void:
	play_sfx("roar")
	play_music("boss")


func _on_boss_ended(victory: bool) -> void:
	if victory:
		play_sfx("victory")
	play_music(_level_music)
