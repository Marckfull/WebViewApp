extends Node
## ShopMenu — a loja do mercador Corvo (autoload, §3.5). UI construída por código,
## no padrão da Forja/Alquimia/Santuário. Compra consumíveis e recursos com Ecos;
## deduz de state["aria"]["ecos"] e credita na bolsa certa (consumables/resources).

var _layer: CanvasLayer
var _greeting: Label
var _ecos_lbl: Label
var _rows: VBoxContainer
var _data: ShopData
var _open: bool = false

func _ready() -> void:
	_build_ui()

func open(data: ShopData) -> void:
	if data == null:
		return
	_data = data
	_open = true
	GameConfig.gameplay_locked = true
	_greeting.text = data.greeting
	_build_rows()
	_refresh()
	_layer.visible = true

func close() -> void:
	_open = false
	_layer.visible = false
	GameConfig.gameplay_locked = false

func _ecos() -> int:
	return int(SaveManager.state["aria"]["ecos"])

func _buy(index: int) -> void:
	if _data == null or index < 0 or index >= _data.offer_count():
		return
	var res := Shop.try_buy(_data.prices[index], _ecos())
	if not res["ok"]:
		return
	SaveManager.state["aria"]["ecos"] = int(res["ecos"])
	var bag_key: String = _data.bags[index]
	var item: StringName = _data.item_ids[index]
	Consumables.grant(item, SaveManager.state[bag_key], 1)
	GameEvents.ecos_changed.emit(int(res["ecos"]))
	# Poções aparecem no HUD; recursos não têm slot, mas o sinal é inócuo.
	GameEvents.consumable_changed.emit(item, Consumables.count(item, SaveManager.state[bag_key]))
	SaveManager.save_game()
	_refresh()

## Recria as linhas de oferta (variam por loja).
func _build_rows() -> void:
	for c in _rows.get_children():
		c.queue_free()
	for i in _data.offer_count():
		var row := HBoxContainer.new()
		row.custom_minimum_size = Vector2(300, 28)
		var name_lbl := Label.new()
		name_lbl.custom_minimum_size = Vector2(210, 0)
		name_lbl.add_theme_color_override("font_color", Color(0.82, 0.84, 0.9))
		name_lbl.text = "%s — %d Ecos" % [_data.labels[i], _data.prices[i]]
		row.add_child(name_lbl)
		var buy := Button.new()
		buy.custom_minimum_size = Vector2(80, 26)
		buy.text = "Comprar"
		var idx := i
		buy.pressed.connect(func() -> void: _buy(idx))
		row.add_child(buy)
		_rows.add_child(row)

func _refresh() -> void:
	_ecos_lbl.text = "Ecos: %d" % _ecos()
	var rows := _rows.get_children()
	for i in rows.size():
		var buy := (rows[i] as HBoxContainer).get_child(1) as Button
		buy.disabled = not Shop.can_afford(_data.prices[i], _ecos())

func _build_ui() -> void:
	_layer = CanvasLayer.new()
	_layer.layer = 12
	_layer.visible = false
	add_child(_layer)

	var panel := Panel.new()
	panel.set_anchors_preset(Control.PRESET_CENTER)
	panel.custom_minimum_size = Vector2(340, 220)
	panel.size = Vector2(340, 220)
	panel.position = Vector2(-170, -110)
	_layer.add_child(panel)

	var head := Label.new()
	head.position = Vector2(12, 8)
	head.text = "Corvo, o Mercador"
	head.add_theme_color_override("font_color", Color(0.7, 0.62, 0.85))
	panel.add_child(head)

	_greeting = Label.new()
	_greeting.position = Vector2(12, 30)
	_greeting.custom_minimum_size = Vector2(316, 0)
	_greeting.autowrap_mode = TextServer.AUTOWRAP_WORD_SMART
	_greeting.add_theme_color_override("font_color", Color(0.72, 0.74, 0.8))
	panel.add_child(_greeting)

	_ecos_lbl = Label.new()
	_ecos_lbl.position = Vector2(12, 84)
	_ecos_lbl.add_theme_color_override("font_color", Color(0.9, 0.85, 0.5))
	panel.add_child(_ecos_lbl)

	_rows = VBoxContainer.new()
	_rows.position = Vector2(12, 108)
	_rows.custom_minimum_size = Vector2(316, 0)
	panel.add_child(_rows)

	var close_btn := Button.new()
	close_btn.position = Vector2(12, 184)
	close_btn.custom_minimum_size = Vector2(316, 28)
	close_btn.text = "Até a próxima estrada"
	close_btn.pressed.connect(close)
	panel.add_child(close_btn)
