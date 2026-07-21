extends Node
## CutsceneManager — reproduz cutscenes narradas (autoload, §2). Reutiliza o padrão
## do prólogo: fade + páginas que avançam por toque/Interagir, travando a jogabilidade.
## Cutscenes "once" são marcadas como vistas no save (não repetem).

var _layer: CanvasLayer
var _fade: ColorRect
var _text: Label
var _hint: Label

var _pages: Array = []
var _idx: int = 0
var _active: bool = false
var _current_id: StringName = &""

func _ready() -> void:
	_build_ui()

func has_seen(id: StringName) -> bool:
	return Inventory.has(id, SaveManager.state["world"].get("cutscenes_seen", []))

## Toca só se ainda não foi vista; marca como vista e salva. Retorna true se tocou.
func play_once(data: CutsceneData) -> bool:
	if data == null or has_seen(data.id):
		return false
	Inventory.grant(data.id, SaveManager.state["world"]["cutscenes_seen"])
	SaveManager.save_game()
	play(data)
	return true

func play(data: CutsceneData) -> void:
	if data == null or data.pages.is_empty():
		return
	_pages = data.pages
	_idx = 0
	_current_id = data.id
	_active = true
	GameConfig.gameplay_locked = true
	_layer.visible = true
	_show_page()

func _input(event: InputEvent) -> void:
	if not _active:
		return
	var go := event.is_action_pressed("interact") or event.is_action_pressed("attack")
	if event is InputEventScreenTouch and event.pressed:
		go = true
	if go:
		get_viewport().set_input_as_handled()
		_advance()

func _advance() -> void:
	_idx += 1
	if _idx >= _pages.size():
		_finish()
	else:
		_show_page()

func _show_page() -> void:
	_text.text = String(_pages[_idx])
	_text.modulate.a = 0.0
	create_tween().tween_property(_text, "modulate:a", 1.0, 0.5)

func _finish() -> void:
	_active = false
	_layer.visible = false
	GameEvents.cutscene_finished.emit(_current_id)
	# Desbloqueia no próximo frame: o mesmo toque que fechou não deve reagir no jogo.
	await get_tree().process_frame
	GameConfig.gameplay_locked = false

func _build_ui() -> void:
	_layer = CanvasLayer.new()
	_layer.layer = 22
	_layer.visible = false
	add_child(_layer)

	_fade = ColorRect.new()
	_fade.color = Color(0.02, 0.03, 0.05, 0.92)
	_fade.set_anchors_preset(Control.PRESET_FULL_RECT)
	_fade.mouse_filter = Control.MOUSE_FILTER_IGNORE
	_layer.add_child(_fade)

	_text = Label.new()
	_text.set_anchors_preset(Control.PRESET_CENTER)
	_text.position = Vector2(-260, -40)
	_text.custom_minimum_size = Vector2(520, 0)
	_text.horizontal_alignment = HORIZONTAL_ALIGNMENT_CENTER
	_text.autowrap_mode = TextServer.AUTOWRAP_WORD_SMART
	_text.add_theme_color_override("font_color", Color(0.88, 0.92, 1.0))
	_layer.add_child(_text)

	_hint = Label.new()
	_hint.set_anchors_preset(Control.PRESET_CENTER)
	_hint.position = Vector2(-260, 64)
	_hint.custom_minimum_size = Vector2(520, 0)
	_hint.horizontal_alignment = HORIZONTAL_ALIGNMENT_CENTER
	_hint.add_theme_color_override("font_color", Color(0.5, 0.55, 0.65))
	_hint.text = "toque para continuar"
	_layer.add_child(_hint)
