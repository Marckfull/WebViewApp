extends Node
## ShopMenu — a loja do mercador Corvo (autoload, §3.5). Três modos: Comprar
## (ofertas do ShopData), Vender (esvazia consumíveis/recursos por Ecos) e
## Recomprar (rebate o que foi vendido nesta visita). UI construída por código.

var _layer: CanvasLayer
var _greeting: Label
var _ecos_lbl: Label
var _rows: VBoxContainer
var _mode_btns: Dictionary = {}
var _data: ShopData
var _mode: String = "buy"
var _buyback: Array = []   ## itens vendidos nesta visita: {id, bag, value}

func _ready() -> void:
	_build_ui()

func open(data: ShopData) -> void:
	if data == null:
		return
	_data = data
	_mode = "buy"
	_buyback = []
	GameConfig.gameplay_locked = true
	_greeting.text = data.greeting
	_redraw()
	_layer.visible = true

func close() -> void:
	_layer.visible = false
	GameConfig.gameplay_locked = false

func _ecos() -> int:
	return int(SaveManager.state["aria"]["ecos"])

func _set_mode(m: String) -> void:
	_mode = m
	_redraw()

# --- Ações --------------------------------------------------------------------

func _buy(index: int) -> void:
	if _data == null or index < 0 or index >= _data.offer_count():
		return
	var res := Shop.try_buy(_data.prices[index], _ecos())
	if not res["ok"]:
		return
	SaveManager.state["aria"]["ecos"] = int(res["ecos"])
	_grant(_data.item_ids[index], _data.bags[index])
	SaveManager.save_game()
	_redraw()

func _sell(id: String, bag_key: String) -> void:
	var bag: Dictionary = SaveManager.state[bag_key]
	if int(bag.get(id, 0)) <= 0:
		return
	Consumables.consume(StringName(id), bag)
	var val := Shop.sell_value(StringName(id))
	SaveManager.state["aria"]["ecos"] = _ecos() + val
	GameEvents.ecos_changed.emit(_ecos())
	GameEvents.consumable_changed.emit(StringName(id), Consumables.count(StringName(id), bag))
	_buyback.append({"id": id, "bag": bag_key, "value": val})
	SaveManager.save_game()
	_redraw()

func _rebuy(index: int) -> void:
	if index < 0 or index >= _buyback.size():
		return
	var entry: Dictionary = _buyback[index]
	var res := Shop.try_buy(int(entry["value"]), _ecos())
	if not res["ok"]:
		return
	SaveManager.state["aria"]["ecos"] = int(res["ecos"])
	_grant(StringName(entry["id"]), String(entry["bag"]))
	_buyback.remove_at(index)
	SaveManager.save_game()
	_redraw()

func _grant(id: StringName, bag_key: String) -> void:
	Consumables.grant(id, SaveManager.state[bag_key], 1)
	GameEvents.ecos_changed.emit(_ecos())
	GameEvents.consumable_changed.emit(id, Consumables.count(id, SaveManager.state[bag_key]))

# --- Linhas por modo ----------------------------------------------------------

func _sellable_entries() -> Array:
	var out: Array = []
	for bag_key in ["consumables", "resources"]:
		var bag: Dictionary = SaveManager.state[bag_key]
		for id in bag:
			if int(bag[id]) > 0:
				out.append({"id": String(id), "bag": bag_key})
	return out

func _redraw() -> void:
	_ecos_lbl.text = "Ecos: %d" % _ecos()
	for key in _mode_btns:
		(_mode_btns[key] as Button).disabled = (key == _mode)
	for c in _rows.get_children():
		c.queue_free()
	match _mode:
		"buy":
			_build_buy_rows()
		"sell":
			_build_sell_rows()
		"buyback":
			_build_buyback_rows()

func _build_buy_rows() -> void:
	for i in _data.offer_count():
		var idx := i
		var btn := _add_row("%s — %d Ecos" % [_data.labels[i], _data.prices[i]], "Comprar",
			func() -> void: _buy(idx))
		btn.disabled = not Shop.can_afford(_data.prices[i], _ecos())

func _build_sell_rows() -> void:
	var entries := _sellable_entries()
	if entries.is_empty():
		_add_note("Nada para vender.")
		return
	for e in entries:
		var id: String = e["id"]
		var bag_key: String = e["bag"]
		var n := int(SaveManager.state[bag_key].get(id, 0))
		_add_row("%s x%d — vale %d" % [ItemNames.label(StringName(id)), n, Shop.sell_value(StringName(id))],
			"Vender", func() -> void: _sell(id, bag_key))

func _build_buyback_rows() -> void:
	if _buyback.is_empty():
		_add_note("Nada vendido nesta visita.")
		return
	for i in _buyback.size():
		var idx := i
		var entry: Dictionary = _buyback[i]
		var btn := _add_row("%s — %d Ecos" % [ItemNames.label(StringName(entry["id"])), int(entry["value"])],
			"Recomprar", func() -> void: _rebuy(idx))
		btn.disabled = not Shop.can_afford(int(entry["value"]), _ecos())

func _add_row(text: String, btn_text: String, cb: Callable) -> Button:
	var row := HBoxContainer.new()
	row.custom_minimum_size = Vector2(316, 26)
	var lbl := Label.new()
	lbl.custom_minimum_size = Vector2(210, 0)
	lbl.add_theme_color_override("font_color", Color(0.82, 0.84, 0.9))
	lbl.text = text
	row.add_child(lbl)
	var btn := Button.new()
	btn.custom_minimum_size = Vector2(96, 24)
	btn.text = btn_text
	btn.pressed.connect(cb)
	row.add_child(btn)
	_rows.add_child(row)
	return btn

func _add_note(text: String) -> void:
	var l := Label.new()
	l.text = text
	l.add_theme_color_override("font_color", Color(0.6, 0.62, 0.68))
	_rows.add_child(l)

# --- UI -----------------------------------------------------------------------

func _build_ui() -> void:
	_layer = CanvasLayer.new()
	_layer.layer = 12
	_layer.visible = false
	add_child(_layer)

	var panel := Panel.new()
	panel.set_anchors_preset(Control.PRESET_CENTER)
	panel.custom_minimum_size = Vector2(360, 270)
	panel.size = Vector2(360, 270)
	panel.position = Vector2(-180, -135)
	_layer.add_child(panel)

	var head := Label.new()
	head.position = Vector2(12, 8)
	head.text = "Corvo, o Mercador"
	head.add_theme_color_override("font_color", Color(0.7, 0.62, 0.85))
	panel.add_child(head)

	_greeting = Label.new()
	_greeting.position = Vector2(12, 30)
	_greeting.custom_minimum_size = Vector2(336, 0)
	_greeting.autowrap_mode = TextServer.AUTOWRAP_WORD_SMART
	_greeting.add_theme_color_override("font_color", Color(0.72, 0.74, 0.8))
	panel.add_child(_greeting)

	var modes := HBoxContainer.new()
	modes.position = Vector2(12, 92)
	modes.add_theme_constant_override("separation", 6)
	panel.add_child(modes)
	_mode_btns["buy"] = _mode_button(modes, "Comprar", "buy")
	_mode_btns["sell"] = _mode_button(modes, "Vender", "sell")
	_mode_btns["buyback"] = _mode_button(modes, "Recomprar", "buyback")

	_ecos_lbl = Label.new()
	_ecos_lbl.position = Vector2(12, 122)
	_ecos_lbl.add_theme_color_override("font_color", Color(0.9, 0.85, 0.5))
	panel.add_child(_ecos_lbl)

	_rows = VBoxContainer.new()
	_rows.position = Vector2(12, 146)
	_rows.custom_minimum_size = Vector2(336, 0)
	panel.add_child(_rows)

	var close_btn := Button.new()
	close_btn.position = Vector2(12, 236)
	close_btn.custom_minimum_size = Vector2(336, 26)
	close_btn.text = "Até a próxima estrada"
	close_btn.pressed.connect(close)
	panel.add_child(close_btn)

func _mode_button(parent: Control, text: String, mode: String) -> Button:
	var b := Button.new()
	b.text = text
	b.custom_minimum_size = Vector2(106, 26)
	b.pressed.connect(func() -> void: _set_mode(mode))
	parent.add_child(b)
	return b
