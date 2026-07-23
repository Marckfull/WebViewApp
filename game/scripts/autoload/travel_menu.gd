extends Node
## TravelMenu — fast-travel entre santuários descobertos (autoload, §3.1/§3.5).
## Aberto pela Canção do Retorno. Lista os santuários já visitados e teleporta
## Aria para o escolhido (troca de cena se necessário).

var _layer: CanvasLayer
var _list: VBoxContainer
var _open: bool = false

func _ready() -> void:
	_build_ui()

func open() -> void:
	_open = true
	GameConfig.gameplay_locked = true
	_rebuild_list()
	_layer.visible = true

func close() -> void:
	_open = false
	_layer.visible = false
	GameConfig.gameplay_locked = false

func _rebuild_list() -> void:
	for c in _list.get_children():
		c.queue_free()
	var shrines: Dictionary = SaveManager.state["world"].get("shrines", {})
	if shrines.is_empty():
		var l := Label.new()
		l.text = "Nenhum santuário descoberto ainda."
		_list.add_child(l)
	else:
		for key in shrines:
			var info: Dictionary = shrines[key]
			var b := Button.new()
			b.text = String(info.get("name", key))
			b.custom_minimum_size = Vector2(280, 28)
			b.pressed.connect(_travel.bind(info))
			_list.add_child(b)
	var close_btn := Button.new()
	close_btn.text = "Cancelar"
	close_btn.custom_minimum_size = Vector2(280, 26)
	close_btn.pressed.connect(close)
	_list.add_child(close_btn)

func _travel(info: Dictionary) -> void:
	var target_scene := String(info.get("scene", ""))
	var pos := Vector2(info.get("x", 0.0), info.get("y", 0.0))
	close()
	if target_scene == "":
		return
	var cs := get_tree().current_scene
	if cs and cs.scene_file_path == target_scene:
		var player := get_tree().get_first_node_in_group("player") as Node2D
		if player:
			player.global_position = pos
	else:
		GameConfig.next_spawn = pos
		GameConfig.has_next_spawn = true
		get_tree().change_scene_to_file(target_scene)

func _build_ui() -> void:
	_layer = CanvasLayer.new()
	_layer.layer = 12
	_layer.visible = false
	add_child(_layer)

	var panel := Panel.new()
	panel.set_anchors_preset(Control.PRESET_CENTER)
	panel.custom_minimum_size = Vector2(300, 220)
	panel.size = Vector2(300, 220)
	panel.position = Vector2(-150, -110)
	_layer.add_child(panel)

	var head := Label.new()
	head.position = Vector2(12, 8)
	head.text = "Canção do Retorno"
	head.add_theme_color_override("font_color", Color(0.4, 0.85, 1))
	panel.add_child(head)

	_list = VBoxContainer.new()
	_list.position = Vector2(12, 34)
	_list.add_theme_constant_override("separation", 4)
	panel.add_child(_list)
