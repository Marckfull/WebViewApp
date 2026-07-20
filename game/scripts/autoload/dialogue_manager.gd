extends Node
## DialogueManager — runtime de diálogo enxuto (autoload).
##
## Mostra um balão (construído por código para evitar dependência de cena) e
## trava a jogabilidade enquanto ativo. Avança com Interagir ou toque na tela.
## Em produção pode ser trocado pelo godot_dialogue_manager (MIT) mantendo a
## chamada start(DialogueData) — ver catálogo de recursos §7.

var _layer: CanvasLayer
var _panel: Panel
var _speaker_label: Label
var _text_label: Label

var _lines: Array[String] = []
var _index: int = 0
var _active: bool = false
var _current_id: StringName = &""

func _ready() -> void:
	_build_ui()

func is_active() -> bool:
	return _active

func start(data: DialogueData) -> void:
	if data == null or data.lines.is_empty():
		return
	_lines = data.lines
	_index = 0
	_current_id = data.id
	_active = true
	_speaker_label.text = data.speaker
	GameConfig.gameplay_locked = true
	GameEvents.dialogue_started.emit()
	_show_line()
	_layer.visible = true

func _input(event: InputEvent) -> void:
	if not _active:
		return
	var advance := event.is_action_pressed("interact")
	if event is InputEventScreenTouch and event.pressed:
		advance = true
	if advance:
		get_viewport().set_input_as_handled()
		_advance()

func _advance() -> void:
	_index += 1
	if _index >= _lines.size():
		_finish()
	else:
		_show_line()

func _show_line() -> void:
	_text_label.text = _lines[_index]

func _finish() -> void:
	_active = false
	_layer.visible = false
	GameEvents.dialogue_finished.emit(_current_id)
	# Desbloqueia só no próximo frame: o mesmo toque/tecla que fechou o diálogo
	# não deve reabri-lo via polling do Player nesse mesmo frame.
	await get_tree().process_frame
	GameConfig.gameplay_locked = false

func _build_ui() -> void:
	_layer = CanvasLayer.new()
	_layer.layer = 10
	_layer.visible = false
	add_child(_layer)

	_panel = Panel.new()
	_panel.set_anchors_preset(Control.PRESET_BOTTOM_WIDE)
	_panel.offset_left = 16
	_panel.offset_right = -16
	_panel.offset_top = -110
	_panel.offset_bottom = -12
	_layer.add_child(_panel)

	_speaker_label = Label.new()
	_speaker_label.position = Vector2(12, 8)
	_speaker_label.add_theme_color_override("font_color", Color(0.95, 0.85, 0.5))
	_panel.add_child(_speaker_label)

	_text_label = Label.new()
	_text_label.position = Vector2(12, 32)
	_text_label.autowrap_mode = TextServer.AUTOWRAP_WORD_SMART
	_text_label.custom_minimum_size = Vector2(0, 60)
	_text_label.set_anchors_preset(Control.PRESET_TOP_WIDE)
	_text_label.offset_left = 12
	_text_label.offset_right = -12
	_text_label.offset_top = 30
	_panel.add_child(_text_label)
