extends Control
## Cutscene genérica entre atos: mostra as falas de GameState.pending_cutscene
## com fade e retorna à cena de GameState.cutscene_return ao terminar.

var _lines: PackedStringArray = []
var _return := ""
var _index := -1
var _transitioning := false

@onready var slide_label: Label = %SlideLabel
@onready var skip_button: Button = %SkipButton
@onready var tap_area: Button = %TapArea


func _ready() -> void:
	_lines = GameState.pending_cutscene
	_return = GameState.cutscene_return
	GameState.pending_cutscene = PackedStringArray()
	GameState.cutscene_return = ""
	if _lines.is_empty():
		_finish()
		return
	AudioManager.play_level_music("crypt")
	skip_button.pressed.connect(_finish)
	tap_area.pressed.connect(_advance)
	_advance()


func _process(_delta: float) -> void:
	if Input.is_action_just_pressed("interact") \
			or Input.is_action_just_pressed("attack"):
		_advance()


func _advance() -> void:
	if _transitioning:
		return
	_index += 1
	if _index >= _lines.size():
		_finish()
		return
	_transitioning = true
	AudioManager.play_sfx("blip")
	var tween := create_tween()
	tween.tween_property(slide_label, "modulate:a", 0.0, 0.25)
	tween.tween_callback(func() -> void: slide_label.text = _lines[_index])
	tween.tween_property(slide_label, "modulate:a", 1.0, 0.5)
	tween.tween_callback(func() -> void: _transitioning = false)


func _finish() -> void:
	AudioManager.play_sfx("melody_return")
	if _return.is_empty():
		_return = "res://world/village.tscn"
	get_tree().change_scene_to_file(_return)
