extends Node
## AlchemyMenu — a bancada da alquimista (autoload, §3.5). Converte ervas (3 tiers)
## e itens de forrageio (madeira, cogumelo, peixe, inseto) em poções, elixir e
## refeições. Uma linha por receita de Alchemy.RECIPES; UI construída por código.

var _layer: CanvasLayer
var _list: VBoxContainer
var _rows: Array = []      ## [{recipe, button, label}] para atualizar estado
var _open: bool = false

func _ready() -> void:
	_build_ui()

func open() -> void:
	_open = true
	GameConfig.gameplay_locked = true
	_refresh()
	_layer.visible = true

func close() -> void:
	_open = false
	_layer.visible = false
	GameConfig.gameplay_locked = false

func _brew(recipe: Dictionary) -> void:
	var resources: Dictionary = SaveManager.state["resources"]
	if not Alchemy.can_make(recipe, resources):
		return
	Alchemy.spend_inputs(recipe, resources)
	var out_bag: String = recipe["output_bag"]
	var out_id: StringName = recipe["output"]
	Consumables.grant(out_id, SaveManager.state[out_bag], 1)
	if out_bag == "consumables":
		GameEvents.consumable_changed.emit(
			out_id, Consumables.count(out_id, SaveManager.state["consumables"]))
	SaveManager.save_game()
	_refresh()

func _refresh() -> void:
	var resources: Dictionary = SaveManager.state["resources"]
	for row in _rows:
		var recipe: Dictionary = row["recipe"]
		var can: bool = Alchemy.can_make(recipe, resources)
		row["label"].text = "%s\n%s" % [recipe["label"], Alchemy.inputs_text(recipe)]
		row["button"].disabled = not can

func _build_ui() -> void:
	_layer = CanvasLayer.new()
	_layer.layer = 12
	_layer.visible = false
	add_child(_layer)

	var panel := Panel.new()
	panel.set_anchors_preset(Control.PRESET_CENTER)
	panel.custom_minimum_size = Vector2(340, 240)
	panel.size = Vector2(340, 240)
	panel.position = Vector2(-170, -120)
	_layer.add_child(panel)

	var head := Label.new()
	head.position = Vector2(12, 8)
	head.text = "Bancada da Alquimista"
	head.add_theme_color_override("font_color", Color(0.5, 0.85, 0.6))
	panel.add_child(head)

	_list = VBoxContainer.new()
	_list.position = Vector2(12, 34)
	_list.custom_minimum_size = Vector2(316, 160)
	_list.add_theme_constant_override("separation", 6)
	panel.add_child(_list)

	for recipe in Alchemy.RECIPES:
		var row := HBoxContainer.new()
		row.add_theme_constant_override("separation", 8)
		_list.add_child(row)

		var info := Label.new()
		info.custom_minimum_size = Vector2(210, 30)
		info.add_theme_color_override("font_color", Color(0.8, 0.82, 0.86))
		info.add_theme_font_size_override("font_size", 12)
		row.add_child(info)

		var btn := Button.new()
		btn.custom_minimum_size = Vector2(96, 30)
		btn.text = "Preparar"
		btn.pressed.connect(_brew.bind(recipe))
		row.add_child(btn)

		_rows.append({"recipe": recipe, "button": btn, "label": info})

	var close_btn := Button.new()
	close_btn.position = Vector2(12, 202)
	close_btn.custom_minimum_size = Vector2(316, 28)
	close_btn.text = "Sair"
	close_btn.pressed.connect(close)
	panel.add_child(close_btn)
