extends Node
## Toca SFX (pool de players) e música com loop. A música continua
## durante pausas (diálogo/inventário) e troca sozinha em lutas de boss.

const SFX_PLAYERS := 8
const SFX_DIR := "res://assets/audio/sfx/"
const MUSIC_DIR := "res://assets/audio/music/"

var _sfx_pool: Array[AudioStreamPlayer] = []
var _music_player: AudioStreamPlayer
var _level_music := ""
var _current_music := ""
var _cache: Dictionary = {}


func _ready() -> void:
	process_mode = Node.PROCESS_MODE_ALWAYS
	for i in SFX_PLAYERS:
		var p := AudioStreamPlayer.new()
		p.volume_db = -6.0
		add_child(p)
		_sfx_pool.append(p)
	_music_player = AudioStreamPlayer.new()
	_music_player.volume_db = -13.0
	add_child(_music_player)
	GameEvents.player_died.connect(_on_player_died)
	GameEvents.shrine_rested.connect(play_sfx.bind("shrine"))
	GameEvents.boss_engaged.connect(_on_boss_engaged)
	GameEvents.boss_ended.connect(_on_boss_ended)


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
