extends Control
## Abertura do Ato 1: o Silêncio chega a Pedra-Alva.
## Slides de texto com fade; toque/interact avança, Pular encerra.

const SLIDES := [
	"Lirael era um reino de canções.\nCada pedra, cada rio, cada pessoa\n"
			+ "tinha a sua nota na Canção do Mundo.",
	"Então veio o Silêncio.\n\nUma névoa que apaga sons, cores\n"
			+ "e memórias — e transforma pessoas\nem cascas vazias: os Ecoados.",
	"Numa manhã cinzenta, o Silêncio\nengoliu a vila de Pedra-Alva.\n\n"
			+ "E levou Lys.",
	"Aria — cartógrafa, irmã, surda de\num ouvido — descobriu que seu "
			+ "ouvido\nsurdo escuta o que ninguém mais pode:\n\no mundo pedindo ajuda.",
]

var _index := -1
var _transitioning := false

@onready var slide_label: Label = %SlideLabel
@onready var skip_button: Button = %SkipButton
@onready var tap_area: Button = %TapArea


func _ready() -> void:
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
	if _index >= SLIDES.size():
		_finish()
		return
	_transitioning = true
	AudioManager.play_sfx("blip")
	var tween := create_tween()
	tween.tween_property(slide_label, "modulate:a", 0.0, 0.25)
	tween.tween_callback(func() -> void: slide_label.text = SLIDES[_index])
	tween.tween_property(slide_label, "modulate:a", 1.0, 0.5)
	tween.tween_callback(func() -> void: _transitioning = false)


func _finish() -> void:
	AudioManager.play_sfx("melody_return")
	get_tree().change_scene_to_file("res://world/village.tscn")
