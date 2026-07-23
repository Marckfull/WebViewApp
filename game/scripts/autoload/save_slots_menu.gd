extends Node
## SaveSlotsMenu — 3 slots manuais de save/load (autoload, §3.6). Abre em modo
## "salvar" (pela pausa) ou "carregar" (pelo menu principal). Mostra um resumo de
## cada slot (Ecos, Memórias). Processa mesmo com o jogo pausado.

enum Mode { SAVE, LOAD }
const SLOTS := [1, 2, 3]

var _mode: Mode = Mode.LOAD
var _layer: CanvasLayer
var _list: VBoxContainer
var _title: Label

func _ready() -> void:
	process_mode = Node.PROCESS_MODE_ALWAYS
	_build_ui()

func open_for_save() -> void:
	_mode = Mode.SAVE
	_open()

func open_for_load() -> void:
	_mode = Mode.LOAD
	_open()

func _open() -> void:
	_title.text = "Salvar em..." if _mode == Mode.SAVE else "Carregar"
	_rebuild()
	_layer.visible = true

func close() -> void:
	_layer.visible = false

func _rebuild() -> void:
	for c in _list.get_children():
		c.queue_free()
	for slot in SLOTS:
		var b := Button.new()
		b.text = _slot_label(slot)
		b.custom_minimum_size = Vector2(280, 30)
		b.disabled = _mode == Mode.LOAD and SaveManager.peek(slot).is_empty()
		var s := slot
		b.pressed.connect(func(): _on_slot(s))
		_list.add_child(b)
	var back := Button.new()
	back.text = "Voltar"
	back.custom_minimum_size = Vector2(280, 26)
	back.pressed.connect(close)
	_list.add_child(back)

func _slot_label(slot: int) -> String:
	var data := SaveManager.peek(slot)
	if data.is_empty():
		return "Slot %d — (vazio)" % slot
	var ecos := int(data.get("aria", {}).get("ecos", 0))
	var mem := int((data.get("memories", []) as Array).size())
	return "Slot %d — Ecos %d · Mem %d/12" % [slot, ecos, mem]

func _on_slot(slot: int) -> void:
	if _mode == Mode.SAVE:
		SaveManager.save_game(slot)
		_rebuild()  # atualiza o resumo
	else:
		_do_load(slot)

func _do_load(slot: int) -> void:
	if not SaveManager.load_game(slot):
		return
	var scene := String(SaveManager.state.get("current_scene", ""))
	var pos: Array = SaveManager.state["aria"].get("position", [0.0, 0.0])
	close()
	get_tree().paused = false
	GameConfig.gameplay_locked = false
	if scene != "":
		GameConfig.next_spawn = Vector2(pos[0], pos[1])
		GameConfig.has_next_spawn = true
		get_tree().change_scene_to_file(scene)

func _build_ui() -> void:
	_layer = CanvasLayer.new()
	_layer.layer = 25
	_layer.visible = false
	add_child(_layer)

	var panel := Panel.new()
	panel.set_anchors_preset(Control.PRESET_CENTER)
	panel.custom_minimum_size = Vector2(300, 200)
	panel.size = Vector2(300, 200)
	panel.position = Vector2(-150, -100)
	_layer.add_child(panel)

	_title = Label.new()
	_title.position = Vector2(12, 8)
	_title.add_theme_color_override("font_color", Color(0.85, 0.9, 1))
	panel.add_child(_title)

	_list = VBoxContainer.new()
	_list.position = Vector2(12, 34)
	_list.add_theme_constant_override("separation", 4)
	panel.add_child(_list)
