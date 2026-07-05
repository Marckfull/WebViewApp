extends CanvasLayer
## Menu de pause: continuar, volumes de música/sons e voltar ao título.

var _open := false

@onready var continue_button: Button = %ContinueButton
@onready var title_button: Button = %TitleButton
@onready var music_label: Label = %MusicLabel
@onready var sfx_label: Label = %SfxLabel
@onready var music_minus: Button = %MusicMinus
@onready var music_plus: Button = %MusicPlus
@onready var sfx_minus: Button = %SfxMinus
@onready var sfx_plus: Button = %SfxPlus
@onready var run_info: Label = %RunInfo


func _ready() -> void:
	visible = false
	continue_button.pressed.connect(_close)
	title_button.pressed.connect(_to_title)
	music_minus.pressed.connect(_change_music.bind(-1))
	music_plus.pressed.connect(_change_music.bind(1))
	sfx_minus.pressed.connect(_change_sfx.bind(-1))
	sfx_plus.pressed.connect(_change_sfx.bind(1))


func _process(_delta: float) -> void:
	if not Input.is_action_just_pressed("pause"):
		return
	if _open:
		_close()
	elif not get_tree().paused:
		_open_menu()


func _open_menu() -> void:
	_open = true
	visible = true
	get_tree().paused = true
	AudioManager.play_sfx("blip")
	_refresh()


func _close() -> void:
	_open = false
	visible = false
	get_tree().paused = false
	AudioManager.play_sfx("blip")


func _to_title() -> void:
	_open = false
	visible = false
	get_tree().paused = false
	get_tree().change_scene_to_file("res://ui/title_screen.tscn")


func _change_music(delta: int) -> void:
	AudioManager.set_music_volume(AudioManager.music_volume + delta)
	AudioManager.play_sfx("blip")
	_refresh()


func _change_sfx(delta: int) -> void:
	AudioManager.set_sfx_volume(AudioManager.sfx_volume + delta)
	AudioManager.play_sfx("blip")
	_refresh()


func _refresh() -> void:
	music_label.text = "Música: %d" % AudioManager.music_volume
	sfx_label.text = "Sons: %d" % AudioManager.sfx_volume
	var info := "Modo " + GameState.difficulty_name()
	if GameState.ng_cycle > 0:
		info += " · Ciclo +%d" % GameState.ng_cycle
	run_info.text = info
