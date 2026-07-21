extends Node
## AlchemyMenu — a alquimista (autoload, §3.5). Converte ervas coletadas em
## Poções de Vigor. UI construída por código, no padrão da Forja/Santuário.

var _layer: CanvasLayer
var _info: Label
var _brew_btn: Button
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

func _brew() -> void:
	var ervas := Consumables.count(&"erva", SaveManager.state["resources"])
	if not Alchemy.can_brew(ervas):
		return
	SaveManager.state["resources"]["erva"] = ervas - Alchemy.ERVAS_POR_POCAO
	Consumables.grant(&"pocao_vigor", SaveManager.state["consumables"], 1)
	GameEvents.consumable_changed.emit(
		&"pocao_vigor", Consumables.count(&"pocao_vigor", SaveManager.state["consumables"]))
	SaveManager.save_game()
	_refresh()

func _refresh() -> void:
	var ervas := Consumables.count(&"erva", SaveManager.state["resources"])
	var pocoes := Consumables.count(&"pocao_vigor", SaveManager.state["consumables"])
	_info.text = "Ervas: %d   Poções: %d\nReceita: %d ervas → 1 Poção de Vigor" % [
		ervas, pocoes, Alchemy.ERVAS_POR_POCAO]
	_brew_btn.disabled = not Alchemy.can_brew(ervas)

func _build_ui() -> void:
	_layer = CanvasLayer.new()
	_layer.layer = 12
	_layer.visible = false
	add_child(_layer)

	var panel := Panel.new()
	panel.set_anchors_preset(Control.PRESET_CENTER)
	panel.custom_minimum_size = Vector2(320, 140)
	panel.size = Vector2(320, 140)
	panel.position = Vector2(-160, -70)
	_layer.add_child(panel)

	var head := Label.new()
	head.position = Vector2(12, 8)
	head.text = "Alquimia"
	head.add_theme_color_override("font_color", Color(0.5, 0.85, 0.6))
	panel.add_child(head)

	_info = Label.new()
	_info.position = Vector2(12, 36)
	_info.add_theme_color_override("font_color", Color(0.8, 0.82, 0.86))
	panel.add_child(_info)

	_brew_btn = Button.new()
	_brew_btn.position = Vector2(12, 90)
	_brew_btn.custom_minimum_size = Vector2(140, 30)
	_brew_btn.text = "Destilar poção"
	_brew_btn.pressed.connect(_brew)
	panel.add_child(_brew_btn)

	var close_btn := Button.new()
	close_btn.position = Vector2(164, 90)
	close_btn.custom_minimum_size = Vector2(140, 30)
	close_btn.text = "Sair"
	close_btn.pressed.connect(close)
	panel.add_child(close_btn)
