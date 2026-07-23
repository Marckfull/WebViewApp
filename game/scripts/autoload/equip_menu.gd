extends Node
## EquipMenu — tela de equipar (autoload, §3.3). Gerencia a build: troca a arma
## entre as possuídas e a armadura/amuleto entre as peças encontradas (incluindo
## "nenhum"). Aberta pelo Santuário; ao voltar, chama o callback recebido.
## UI por código, no padrão da Forja/Alquimia/Loja.

var _layer: CanvasLayer
var _weapon_lbl: Label
var _armor_lbl: Label
var _amulet_lbl: Label
var _on_close: Callable = Callable()

func _ready() -> void:
	_build_ui()

func open(on_close: Callable = Callable()) -> void:
	_on_close = on_close
	GameConfig.gameplay_locked = true
	_refresh()
	_layer.visible = true

func close() -> void:
	_layer.visible = false
	if _on_close.is_valid():
		_on_close.call()
	else:
		GameConfig.gameplay_locked = false

func _player() -> Node:
	return get_tree().get_first_node_in_group("player")

# --- Arma ---------------------------------------------------------------------

func _cycle_weapon() -> void:
	var p := _player()
	if p == null:
		return
	var ws: Array = p.list_weapons()
	if ws.size() <= 1:
		return
	var cur: StringName = p.current_weapon_id()
	var idx := 0
	for i in ws.size():
		if ws[i].id == cur:
			idx = i
			break
	p.equip_by_id(ws[(idx + 1) % ws.size()].id)
	_refresh()

# --- Armadura / Amuleto -------------------------------------------------------

## Opções de um slot: "nenhum" (null) + as peças possuídas daquele slot.
func _slot_options(slot_enum: int) -> Array:
	var out: Array = [null]
	for id in SaveManager.state.get("equipment_owned", []):
		var e := Equipment.by_id(StringName(id))
		if e and e.slot == slot_enum:
			out.append(e)
	return out

func _cycle_slot(slot_key: String, slot_enum: int) -> void:
	var opts := _slot_options(slot_enum)
	if opts.size() <= 1:
		return
	var cur_id := String(SaveManager.state["equipment"].get(slot_key, ""))
	var idx := 0
	for i in opts.size():
		var oid := "" if opts[i] == null else String((opts[i] as EquipmentData).id)
		if oid == cur_id:
			idx = i
			break
	var nxt: Variant = opts[(idx + 1) % opts.size()]
	SaveManager.state["equipment"][slot_key] = "" if nxt == null else String((nxt as EquipmentData).id)
	SaveManager.save_game()
	var p := _player()
	if p and p.has_method("apply_equipment"):
		p.apply_equipment()
	_refresh()

# --- UI -----------------------------------------------------------------------

func _refresh() -> void:
	var p := _player()
	if p and p.has_method("current_weapon_id"):
		var wid: StringName = p.current_weapon_id()
		var wd := _weapon_by_id(p, wid)
		if wd:
			_weapon_lbl.text = "Arma: %s\n  dano %d · postura %d · alcance %d" % [
				wd.display_name, int(wd.base_damage), int(wd.poise_damage), int(wd.reach)]
		else:
			_weapon_lbl.text = "Arma: —"
	_armor_lbl.text = "Armadura: " + _piece_text("armor")
	_amulet_lbl.text = "Amuleto: " + _piece_text("amulet")

func _weapon_by_id(p: Node, id: StringName) -> WeaponData:
	for w in p.list_weapons():
		if w.id == id:
			return w
	return null

func _piece_text(slot_key: String) -> String:
	var cur_id := String(SaveManager.state["equipment"].get(slot_key, ""))
	if cur_id == "":
		return "Nenhum"
	var e := Equipment.by_id(StringName(cur_id))
	if e == null:
		return "Nenhum"
	var parts: Array = []
	if e.damage_reduction > 0.0:
		parts.append("-%d%% dano" % int(e.damage_reduction * 100.0))
	if e.max_hp_bonus > 0.0:
		parts.append("+%d vida" % int(e.max_hp_bonus))
	if e.stamina_regen_bonus > 0.0:
		parts.append("+%d regen" % int(e.stamina_regen_bonus))
	var suffix := ("  (" + ", ".join(PackedStringArray(parts)) + ")") if not parts.is_empty() else ""
	return e.display_name + suffix

func _build_ui() -> void:
	_layer = CanvasLayer.new()
	_layer.layer = 13
	_layer.visible = false
	add_child(_layer)

	var panel := Panel.new()
	panel.set_anchors_preset(Control.PRESET_CENTER)
	panel.custom_minimum_size = Vector2(360, 240)
	panel.size = Vector2(360, 240)
	panel.position = Vector2(-180, -120)
	_layer.add_child(panel)

	var title := Label.new()
	title.position = Vector2(12, 8)
	title.text = "Equipamento"
	title.add_theme_color_override("font_color", Color(0.7, 0.8, 0.95))
	panel.add_child(title)

	_weapon_lbl = _add_row(panel, 34)
	var wbtn := _add_button(panel, 34, "Trocar arma")
	wbtn.pressed.connect(_cycle_weapon)

	_armor_lbl = _add_row(panel, 96)
	var abtn := _add_button(panel, 96, "Trocar")
	abtn.pressed.connect(_cycle_slot.bind("armor", EquipmentData.Slot.ARMOR))

	_amulet_lbl = _add_row(panel, 140)
	var mbtn := _add_button(panel, 140, "Trocar")
	mbtn.pressed.connect(_cycle_slot.bind("amulet", EquipmentData.Slot.AMULET))

	var close_btn := Button.new()
	close_btn.text = "Voltar"
	close_btn.position = Vector2(12, 200)
	close_btn.custom_minimum_size = Vector2(336, 28)
	close_btn.pressed.connect(close)
	panel.add_child(close_btn)

func _add_row(panel: Panel, y: int) -> Label:
	var l := Label.new()
	l.position = Vector2(12, y)
	l.custom_minimum_size = Vector2(240, 0)
	l.add_theme_color_override("font_color", Color(0.85, 0.87, 0.92))
	panel.add_child(l)
	return l

func _add_button(panel: Panel, y: int, text: String) -> Button:
	var b := Button.new()
	b.text = text
	b.position = Vector2(258, y)
	b.custom_minimum_size = Vector2(90, 26)
	panel.add_child(b)
	return b
