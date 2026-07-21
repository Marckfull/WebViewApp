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
var _difficulty_label: Label
var _telemetry_btn: Button
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
	_layer.add_child(_root_panel)
	var title := _make_label("Pausa", Vector2(12, 8), Color(0.85, 0.9, 1))
	_root_panel.add_child(title)
	_add_button(_root_panel, "Continuar", Vector2(12, 34), _resume)
	_add_button(_root_panel, "Salvar", Vector2(12, 66), func(): SaveSlotsMenu.open_for_save())
	_add_button(_root_panel, "Opções", Vector2(12, 98), func(): _show_options())
	_add_button(_root_panel, "Bestiário", Vector2(12, 130), func(): _show_bestiary())
	_add_button(_root_panel, "Menu principal", Vector2(12, 162), _to_main_menu)

	_options_panel = _make_panel()
	_options_panel.visible = false
	_layer.add_child(_options_panel)
	_difficulty_label = _make_label("Dificuldade: Canção", Vector2(12, 6), Color(0.85, 0.9, 1))
	_options_panel.add_child(_difficulty_label)
	_add_button(_options_panel, "Balada (casual)", Vector2(12, 32), func(): set_difficulty(0))
	_add_button(_options_panel, "Canção (padrão)", Vector2(12, 64), func(): set_difficulty(1))
	_add_button(_options_panel, "Requiem (souls)", Vector2(12, 96), func(): set_difficulty(2))
	_telemetry_btn = _add_button(_options_panel, "Telemetria: OFF", Vector2(12, 128), _toggle_telemetry)
	_add_button(_options_panel, "Voltar", Vector2(12, 164), func(): _show_root())

	_bestiary_panel = _make_panel()
	_bestiary_panel.visible = false
	_layer.add_child(_bestiary_panel)
	var b_title := _make_label("Bestiário", Vector2(12, 8), Color(0.85, 0.9, 1))
	_bestiary_panel.add_child(b_title)
	_bestiary_label = _make_label("", Vector2(12, 34), Color(0.8, 0.82, 0.86))
	_bestiary_label.autowrap_mode = TextServer.AUTOWRAP_WORD_SMART
	_bestiary_label.custom_minimum_size = Vector2(216, 110)
	_bestiary_panel.add_child(_bestiary_label)
	_add_button(_bestiary_panel, "Voltar", Vector2(12, 160), func(): _show_root())

func _show_options() -> void:
	_root_panel.visible = false
	_bestiary_panel.visible = false
	_update_telemetry_label()
	_options_panel.visible = true

func _toggle_telemetry() -> void:
	TelemetryLogger.set_enabled(not TelemetryLogger.is_enabled())
	_update_telemetry_label()

func _update_telemetry_label() -> void:
	_telemetry_btn.text = "Telemetria: %s" % ("ON" if TelemetryLogger.is_enabled() else "OFF")

func _show_bestiary() -> void:
	_root_panel.visible = false
	_options_panel.visible = false
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
	return text.strip_edges()

func _show_root() -> void:
	_options_panel.visible = false
	_bestiary_panel.visible = false
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
