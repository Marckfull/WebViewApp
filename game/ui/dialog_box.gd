extends CanvasLayer
## Caixa de diálogo: pausa o jogo e avança as linhas com interact/attack.
## process_mode ALWAYS para continuar recebendo input durante a pausa.

var _lines: PackedStringArray = []
var _index := 0
var _skip_frame := false

@onready var speaker_label: Label = %SpeakerLabel
@onready var text_label: Label = %TextLabel


func _ready() -> void:
	visible = false
	GameEvents.dialog_requested.connect(open)


func _process(_delta: float) -> void:
	if not visible:
		return
	if _skip_frame:
		# Ignora o mesmo pressionar de "interact" que abriu o diálogo.
		_skip_frame = false
		return
	if Input.is_action_just_pressed("interact") \
			or Input.is_action_just_pressed("attack"):
		_advance()


func open(speaker: String, lines: PackedStringArray) -> void:
	if lines.is_empty() or visible:
		return
	_lines = lines
	_index = 0
	speaker_label.text = speaker
	text_label.text = lines[0]
	visible = true
	_skip_frame = true
	get_tree().paused = true


func _advance() -> void:
	_index += 1
	if _index >= _lines.size():
		visible = false
		get_tree().paused = false
	else:
		text_label.text = _lines[_index]
