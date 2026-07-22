extends Node
## PauseMenu — pausa + opções (autoload, §3.6 acessibilidade).
##
## Esc / botão Start (ou botão touch no HUD) pausa o jogo. Painel de opções ajusta
## a dificuldade (Balada/Canção/Requiem, §3.4) em tempo real. Processa enquanto o
## jogo está pausado (process_mode = ALWAYS).

const MAIN_MENU := "res://scenes/ui/main_menu.tscn"
const DIFFICULTY_NAMES := ["Balada", "Canção", "Requiem"]

var _layer: CanvasLayer
var _root_panel: Panel
var _options_panel: Panel
var _bestiary_panel: Panel
var _bestiary_label: Label
var _inventory_panel: Panel
var _inventory_label: Label
var _inventory_grid: GridContainer
var _difficulty_label: Label
var _telemetry_btn: Button
var _language_btn: Button
var _open: bool = false

func _ready() -> void:
	process_mode = Node.PROCESS_MODE_ALWAYS
	_build_ui()

func _input(event: InputEvent) -> void:
	# Não abre pausa durante diálogo/menus que já travam a jogabilidade.
	if event.is_action_pressed("pause") and not _is_blocking_menu_open():
		toggle()
		get_viewport().set_input_as_handled()

func _is_blocking_menu_open() -> bool:
	# Se a jogabilidade já está travada por diálogo/ocarina/santuário, ignora.
	return GameConfig.gameplay_locked and not _open

func toggle() -> void:
	if _open:
		_resume()
	else:
		_pause()

func _pause() -> void:
	_open = true
	_options_panel.visible = false
	_bestiary_panel.visible = false
	_inventory_panel.visible = false
	_root_panel.visible = true
	_layer.visible = true
	get_tree().paused = true

func _resume() -> void:
	_open = false
	_layer.visible = false
	get_tree().paused = false

func _to_main_menu() -> void:
	_resume()
	get_tree().change_scene_to_file(MAIN_MENU)

func set_difficulty(index: int) -> void:
	GameConfig.difficulty = index
	_difficulty_label.text = "Dificuldade: %s" % DIFFICULTY_NAMES[index]

func _build_ui() -> void:
	_layer = CanvasLayer.new()
	_layer.layer = 20
	_layer.visible = false
	add_child(_layer)

	_root_panel = _make_panel()
	_root_panel.custom_minimum_size = Vector2(240, 244)
	_root_panel.size = Vector2(240, 244)
	_root_panel.position = Vector2(-120, -122)
	_layer.add_child(_root_panel)
	var title := _make_label(Locale.t("PAUSE"), Vector2(12, 8), Color(0.85, 0.9, 1))
	_root_panel.add_child(title)
	_add_button(_root_panel, Locale.t("PAUSE_RESUME"), Vector2(12, 34), _resume)
	_add_button(_root_panel, Locale.t("PAUSE_INVENTORY"), Vector2(12, 66), func(): _show_inventory())
	_add_button(_root_panel, Locale.t("PAUSE_BESTIARY"), Vector2(12, 98), func(): _show_bestiary())
	_add_button(_root_panel, Locale.t("PAUSE_SAVE"), Vector2(12, 130), func(): SaveSlotsMenu.open_for_save())
	_add_button(_root_panel, Locale.t("PAUSE_OPTIONS"), Vector2(12, 162), func(): _show_options())
	_add_button(_root_panel, Locale.t("PAUSE_MAIN_MENU"), Vector2(12, 194), _to_main_menu)

	_options_panel = _make_panel()
	_options_panel.custom_minimum_size = Vector2(240, 244)
	_options_panel.size = Vector2(240, 244)
	_options_panel.position = Vector2(-120, -122)
	_options_panel.visible = false
	_layer.add_child(_options_panel)
	_difficulty_label = _make_label("Dificuldade: Canção", Vector2(12, 6), Color(0.85, 0.9, 1))
	_options_panel.add_child(_difficulty_label)
	_add_button(_options_panel, "Balada (casual)", Vector2(12, 32), func(): set_difficulty(0))
	_add_button(_options_panel, "Canção (padrão)", Vector2(12, 64), func(): set_difficulty(1))
	_add_button(_options_panel, "Requiem (souls)", Vector2(12, 96), func(): set_difficulty(2))
	_telemetry_btn = _add_button(_options_panel, "Telemetria: OFF", Vector2(12, 128), _toggle_telemetry)
	_language_btn = _add_button(_options_panel, "Idioma: PT", Vector2(12, 160), _toggle_language)
	_add_button(_options_panel, Locale.t("BACK"), Vector2(12, 196), func(): _show_root())

	_bestiary_panel = _make_panel()
	_bestiary_panel.visible = false
	_layer.add_child(_bestiary_panel)
	var b_title := _make_label("Bestiário", Vector2(12, 8), Color(0.85, 0.9, 1))
	_bestiary_panel.add_child(b_title)
	_bestiary_label = _make_label("", Vector2(12, 34), Color(0.8, 0.82, 0.86))
	_bestiary_label.autowrap_mode = TextServer.AUTOWRAP_WORD_SMART
	_bestiary_label.custom_minimum_size = Vector2(216, 110)
	_bestiary_panel.add_child(_bestiary_label)
	_add_button(_bestiary_panel, Locale.t("BACK"), Vector2(12, 160), func(): _show_root())

	_inventory_panel = _make_panel()
	_inventory_panel.custom_minimum_size = Vector2(320, 300)
	_inventory_panel.size = Vector2(320, 300)
	_inventory_panel.position = Vector2(-160, -150)
	_inventory_panel.visible = false
	_layer.add_child(_inventory_panel)
	var i_title := _make_label("Inventário", Vector2(12, 8), Color(0.85, 0.9, 1))
	_inventory_panel.add_child(i_title)
	_inventory_label = _make_label("", Vector2(12, 34), Color(0.82, 0.85, 0.9))
	_inventory_label.autowrap_mode = TextServer.AUTOWRAP_WORD_SMART
	_inventory_label.custom_minimum_size = Vector2(296, 120)
	_inventory_panel.add_child(_inventory_label)
	var bags_title := _make_label("Bolsa", Vector2(12, 150), Color(0.7, 0.78, 0.85))
	_inventory_panel.add_child(bags_title)
	_inventory_grid = GridContainer.new()
	_inventory_grid.columns = 5
	_inventory_grid.position = Vector2(12, 170)
	_inventory_grid.add_theme_constant_override("h_separation", 6)
	_inventory_grid.add_theme_constant_override("v_separation", 6)
	_inventory_panel.add_child(_inventory_grid)
	_add_button(_inventory_panel, Locale.t("BACK"), Vector2(12, 262), func(): _show_root())

func _show_inventory() -> void:
	_root_panel.visible = false
	_options_panel.visible = false
	_bestiary_panel.visible = false
	_inventory_label.text = _inventory_text()
	_populate_inventory_grid()
	_inventory_panel.visible = true

## Preenche a grade greybox com uma célula por item de bolsa (consumíveis + recursos).
func _populate_inventory_grid() -> void:
	for child in _inventory_grid.get_children():
		child.queue_free()
	var s: Dictionary = SaveManager.state
	var cells := ItemIcons.cells_from(s.get("consumables", {}))
	cells.append_array(ItemIcons.cells_from(s.get("resources", {})))
	if cells.is_empty():
		_inventory_grid.add_child(_make_label("—", Vector2.ZERO, Color(0.6, 0.62, 0.66)))
		return
	for cell in cells:
		_inventory_grid.add_child(_make_item_cell(cell["id"], int(cell["count"])))

## Célula: um quadrado colorido (ícone greybox) sobre o nome curto e a quantidade.
func _make_item_cell(id: StringName, count: int) -> Control:
	var box := VBoxContainer.new()
	box.custom_minimum_size = Vector2(52, 44)
	box.add_theme_constant_override("separation", 1)
	var swatch := ColorRect.new()
	swatch.color = ItemIcons.color_for(id)
	swatch.custom_minimum_size = Vector2(24, 24)
	swatch.size_flags_horizontal = Control.SIZE_SHRINK_CENTER
	box.add_child(swatch)
	var name_lbl := Label.new()
	name_lbl.text = ItemIcons.short_label(id)
	name_lbl.add_theme_font_size_override("font_size", 9)
	name_lbl.add_theme_color_override("font_color", Color(0.82, 0.85, 0.9))
	name_lbl.horizontal_alignment = HORIZONTAL_ALIGNMENT_CENTER
	box.add_child(name_lbl)
	var count_lbl := Label.new()
	count_lbl.text = "x%d" % count
	count_lbl.add_theme_font_size_override("font_size", 9)
	count_lbl.add_theme_color_override("font_color", Color(0.7, 0.75, 0.8))
	count_lbl.horizontal_alignment = HORIZONTAL_ALIGNMENT_CENTER
	box.add_child(count_lbl)
	return box

## Monta o resumo do inventário a partir do save (§3.5). Nomes vêm dos recursos
## (armas/equipamento têm display_name) ou de ItemNames (bolsas/itens-chave).
func _inventory_text() -> String:
	var s: Dictionary = SaveManager.state
	var out := "ARMAS: " + _weapon_names(s.get("weapons", [])) + "\n\n"
	var eq: Dictionary = s.get("equipment", {})
	out += "Armadura: " + _equip_name(String(eq.get("armor", ""))) + "\n"
	out += "Amuleto: " + _equip_name(String(eq.get("amulet", ""))) + "\n\n"
	out += "Itens-chave: " + _item_names(s.get("items", [])) + "\n"
	out += "Memórias: %d/12   Ecos: %d" % [
		s.get("memories", []).size(), int(s.get("aria", {}).get("ecos", 0))]
	return out

func _weapon_names(ids: Array) -> String:
	var names := PackedStringArray()
	for wid in ids:
		var w := load("res://data/weapons/%s.tres" % String(wid)) as WeaponData
		names.append(w.display_name if w else String(wid).capitalize())
	return ", ".join(names) if names.size() > 0 else "—"

func _item_names(ids: Array) -> String:
	if ids.is_empty():
		return "—"
	var names := PackedStringArray()
	for it in ids:
		names.append(ItemNames.label(it))
	return ", ".join(names)

func _equip_name(id: String) -> String:
	if id == "":
		return "—"
	var e := Equipment.by_id(StringName(id))
	return e.display_name if e else id.capitalize()


func _show_options() -> void:
	_root_panel.visible = false
	_bestiary_panel.visible = false
	_inventory_panel.visible = false
	_update_telemetry_label()
	_update_language_label()
	_options_panel.visible = true

func _toggle_telemetry() -> void:
	TelemetryLogger.set_enabled(not TelemetryLogger.is_enabled())
	_update_telemetry_label()

func _update_telemetry_label() -> void:
	_telemetry_btn.text = "Telemetria: %s" % ("ON" if TelemetryLogger.is_enabled() else "OFF")

func _toggle_language() -> void:
	Locale.toggle()
	_update_language_label()

func _update_language_label() -> void:
	_language_btn.text = "%s: %s" % [Locale.t("OPT_LANGUAGE"), Locale.short_name()]

func _show_bestiary() -> void:
	_root_panel.visible = false
	_options_panel.visible = false
	_inventory_panel.visible = false
	_bestiary_label.text = _bestiary_text()
	_bestiary_panel.visible = true

func _bestiary_text() -> String:
	var entries: Array = SaveManager.state["bestiary"]
	var bosses: Array = SaveManager.state["world"]["bosses_defeated"]
	if entries.is_empty() and bosses.is_empty():
		return "Nenhum inimigo catalogado ainda.\nDerrote Ecoados para preencher."
	var text := ""
	for id in entries:
		if bosses.has(id):
			continue  # bosses listados à parte, sem duplicar
		text += "• %s\n" % String(id).capitalize()
	for id in bosses:
		text += "★ %s (boss)\n" % String(id).capitalize()
	var items: Array = SaveManager.state.get("items", [])
	var memories: Array = SaveManager.state.get("memories", [])
	text += "\nItens: %d   Memórias: %d/12" % [items.size(), memories.size()]
	var ng: int = int(SaveManager.state.get("ng_cycle", 0))
	if ng > 0:
		text += "   New Game+ %d" % ng
	return text.strip_edges()

func _show_root() -> void:
	_options_panel.visible = false
	_bestiary_panel.visible = false
	_inventory_panel.visible = false
	_root_panel.visible = true

func _make_panel() -> Panel:
	var p := Panel.new()
	p.set_anchors_preset(Control.PRESET_CENTER)
	p.custom_minimum_size = Vector2(240, 200)
	p.size = Vector2(240, 200)
	p.position = Vector2(-120, -100)
	return p

func _make_label(text: String, pos: Vector2, color: Color) -> Label:
	var l := Label.new()
	l.text = text
	l.position = pos
	l.add_theme_color_override("font_color", color)
	return l

func _add_button(parent: Control, text: String, pos: Vector2, cb: Callable) -> Button:
	var b := Button.new()
	b.text = text
	b.position = pos
	b.custom_minimum_size = Vector2(216, 30)
	b.pressed.connect(cb)
	parent.add_child(b)
	return b
