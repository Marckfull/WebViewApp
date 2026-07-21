extends Node
## EndingScreen — desfecho da fatia vertical (autoload). Ao derrotar a Guardiã do
## Eco, apresenta um fecho temático (§2) e volta ao menu. É o "fim da demonstração"
## que fecha o loop do slice (gate da Fase 1, §7).

const MAIN_MENU := "res://scenes/ui/main_menu.tscn"
const GAME_START := "res://scenes/world/cripta_das_guardias.tscn"

var _layer: CanvasLayer
var _fade: ColorRect
var _title: Label
var _subtitle: Label
var _button: Button
var _ngplus_btn: Button

func _ready() -> void:
	process_mode = Node.PROCESS_MODE_ALWAYS
	_build_ui()
	GameEvents.boss_defeated.connect(_on_boss_defeated)

func _on_boss_defeated(boss_id: StringName) -> void:
	if boss_id != &"guardia_do_eco":
		return
	await get_tree().create_timer(1.2).timeout
	_show()

func _show() -> void:
	GameConfig.gameplay_locked = true
	_layer.visible = true
	_fade.color = Color(0, 0, 0, 0)
	_title.modulate.a = 0.0
	_subtitle.modulate.a = 0.0
	_button.modulate.a = 0.0
	_ngplus_btn.modulate.a = 0.0
	_button.disabled = true
	_ngplus_btn.disabled = true
	var t := create_tween()
	t.tween_property(_fade, "color", Color(0.02, 0.03, 0.05, 1.0), 1.2)
	t.tween_property(_title, "modulate:a", 1.0, 0.8)
	t.tween_property(_subtitle, "modulate:a", 1.0, 0.8)
	t.tween_property(_ngplus_btn, "modulate:a", 1.0, 0.4)
	t.tween_property(_button, "modulate:a", 1.0, 0.4)
	t.tween_callback(_enable_buttons)

func _enable_buttons() -> void:
	_button.disabled = false
	_ngplus_btn.disabled = false

func _to_menu() -> void:
	GameConfig.gameplay_locked = false
	_layer.visible = false
	get_tree().change_scene_to_file(MAIN_MENU)

## Novo Jogo+ (§3.6): mantém a progressão, remixa o mundo mais difícil.
func _new_game_plus() -> void:
	GameConfig.gameplay_locked = false
	_layer.visible = false
	SaveManager.start_ng_plus()
	get_tree().change_scene_to_file(GAME_START)

func _build_ui() -> void:
	_layer = CanvasLayer.new()
	_layer.layer = 30
	_layer.visible = false
	add_child(_layer)

	_fade = ColorRect.new()
	_fade.set_anchors_preset(Control.PRESET_FULL_RECT)
	_fade.color = Color(0, 0, 0, 0)
	_fade.mouse_filter = Control.MOUSE_FILTER_IGNORE
	_layer.add_child(_fade)

	_title = Label.new()
	_title.set_anchors_preset(Control.PRESET_CENTER)
	_title.position = Vector2(-200, -60)
	_title.custom_minimum_size = Vector2(400, 0)
	_title.horizontal_alignment = HORIZONTAL_ALIGNMENT_CENTER
	_title.add_theme_font_size_override("font_size", 24)
	_title.add_theme_color_override("font_color", Color(0.4, 0.85, 1.0))
	_title.text = "A Guardiã do Eco silenciou."
	_layer.add_child(_title)

	_subtitle = Label.new()
	_subtitle.set_anchors_preset(Control.PRESET_CENTER)
	_subtitle.position = Vector2(-200, -20)
	_subtitle.custom_minimum_size = Vector2(400, 0)
	_subtitle.horizontal_alignment = HORIZONTAL_ALIGNMENT_CENTER
	_subtitle.autowrap_mode = TextServer.AUTOWRAP_WORD_SMART
	_subtitle.add_theme_color_override("font_color", Color(0.7, 0.75, 0.82))
	_subtitle.text = "Mas o Silêncio continua, e a canção ainda não está completa.\nFim da demonstração de Ecos de Lirael."
	_layer.add_child(_subtitle)

	_ngplus_btn = Button.new()
	_ngplus_btn.set_anchors_preset(Control.PRESET_CENTER)
	_ngplus_btn.position = Vector2(-80, 54)
	_ngplus_btn.custom_minimum_size = Vector2(160, 34)
	_ngplus_btn.text = "Novo Jogo+"
	_ngplus_btn.pressed.connect(_new_game_plus)
	_layer.add_child(_ngplus_btn)

	_button = Button.new()
	_button.set_anchors_preset(Control.PRESET_CENTER)
	_button.position = Vector2(-80, 96)
	_button.custom_minimum_size = Vector2(160, 34)
	_button.text = "Voltar ao Menu"
	_button.pressed.connect(_to_menu)
	_layer.add_child(_button)
