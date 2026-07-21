extends Node
## EndingScreen — desfechos narrativos (autoload, §2).
##
## A Guardiã do Eco (Ato 1) não encerra o jogo: ao cair, sussurra e aponta o
## caminho para o fundo (cutscene guardia_fall), e Aria segue para o Coração Mudo.
## Selene (boss final, Ato 3) traz a ESCOLHA de fim: Silenciar, Completar ou —
## com as 12 Memórias — Lembrar em Coro (final secreto). Depois: Novo Jogo+ / Menu.

const MAIN_MENU := "res://scenes/ui/main_menu.tscn"
const GAME_START := "res://scenes/world/cripta_das_guardias.tscn"
const FALL_CUTSCENE := "res://data/cutscenes/guardia_fall.tres"

var _layer: CanvasLayer
var _fade: ColorRect
var _title: Label
var _subtitle: Label
var _choices: VBoxContainer
var _ngplus_btn: Button
var _menu_btn: Button

func _ready() -> void:
	process_mode = Node.PROCESS_MODE_ALWAYS
	_build_ui()
	GameEvents.boss_defeated.connect(_on_boss_defeated)

func _on_boss_defeated(boss_id: StringName) -> void:
	match boss_id:
		&"guardia_do_eco":
			await _play_guardia_fall()
		&"selene":
			await get_tree().create_timer(1.2).timeout
			_show_choice()

## As palavras da Guardiã ao cair (§2). Não encerra o jogo: aponta o fundo.
func _play_guardia_fall() -> void:
	await get_tree().create_timer(1.2).timeout
	var fall: CutsceneData = load(FALL_CUTSCENE)
	if fall != null:
		CutsceneManager.play(fall)
		await GameEvents.cutscene_finished

## Fase 1 do fecho: apresenta a escolha do fim (§2). Os finais disponíveis
## dependem das Memórias coletadas (o secreto exige as 12).
func _show_choice() -> void:
	GameConfig.gameplay_locked = true
	_title.text = "O Coração Mudo cala."
	_subtitle.text = "Selene se ajoelha. A canção ao contrário hesita. Aria decide como a história termina."
	_menu_btn.visible = false
	_ngplus_btn.visible = false
	_build_choice_buttons()
	_reveal()

func _build_choice_buttons() -> void:
	for c in _choices.get_children():
		c.queue_free()
	var mem: int = SaveManager.state["memories"].size()
	for ending in Endings.available(mem):
		var b := Button.new()
		b.custom_minimum_size = Vector2(300, 32)
		b.text = Endings.title(ending)
		var chosen: StringName = ending
		b.pressed.connect(func() -> void: _choose(chosen))
		_choices.add_child(b)

## Fase 2: registra a escolha, mostra o epílogo e abre Novo Jogo+ / Menu.
func _choose(ending: StringName) -> void:
	SaveManager.state["ending_chosen"] = String(ending)
	SaveManager.save_game()
	for c in _choices.get_children():
		c.queue_free()
	_title.text = Endings.title(ending)
	_subtitle.text = Endings.epilogue(ending)
	_menu_btn.visible = true
	_ngplus_btn.visible = true
	_menu_btn.modulate.a = 0.0
	_ngplus_btn.modulate.a = 0.0
	var t := create_tween()
	t.tween_property(_subtitle, "modulate:a", 1.0, 0.6)
	t.tween_property(_ngplus_btn, "modulate:a", 1.0, 0.4)
	t.tween_property(_menu_btn, "modulate:a", 1.0, 0.4)

func _reveal() -> void:
	_layer.visible = true
	_fade.color = Color(0, 0, 0, 0)
	_title.modulate.a = 0.0
	_subtitle.modulate.a = 0.0
	var t := create_tween()
	t.tween_property(_fade, "color", Color(0.02, 0.03, 0.05, 1.0), 1.2)
	t.tween_property(_title, "modulate:a", 1.0, 0.8)
	t.tween_property(_subtitle, "modulate:a", 1.0, 0.8)

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
	_title.position = Vector2(-200, -96)
	_title.custom_minimum_size = Vector2(400, 0)
	_title.horizontal_alignment = HORIZONTAL_ALIGNMENT_CENTER
	_title.add_theme_font_size_override("font_size", 24)
	_title.add_theme_color_override("font_color", Color(0.4, 0.85, 1.0))
	_layer.add_child(_title)

	_subtitle = Label.new()
	_subtitle.set_anchors_preset(Control.PRESET_CENTER)
	_subtitle.position = Vector2(-220, -56)
	_subtitle.custom_minimum_size = Vector2(440, 0)
	_subtitle.horizontal_alignment = HORIZONTAL_ALIGNMENT_CENTER
	_subtitle.autowrap_mode = TextServer.AUTOWRAP_WORD_SMART
	_subtitle.add_theme_color_override("font_color", Color(0.7, 0.75, 0.82))
	_layer.add_child(_subtitle)

	_choices = VBoxContainer.new()
	_choices.set_anchors_preset(Control.PRESET_CENTER)
	_choices.position = Vector2(-150, 16)
	_choices.custom_minimum_size = Vector2(300, 0)
	_choices.add_theme_constant_override("separation", 8)
	_layer.add_child(_choices)

	_ngplus_btn = Button.new()
	_ngplus_btn.set_anchors_preset(Control.PRESET_CENTER)
	_ngplus_btn.position = Vector2(-80, 60)
	_ngplus_btn.custom_minimum_size = Vector2(160, 34)
	_ngplus_btn.text = "Novo Jogo+"
	_ngplus_btn.visible = false
	_ngplus_btn.pressed.connect(_new_game_plus)
	_layer.add_child(_ngplus_btn)

	_menu_btn = Button.new()
	_menu_btn.set_anchors_preset(Control.PRESET_CENTER)
	_menu_btn.position = Vector2(-80, 102)
	_menu_btn.custom_minimum_size = Vector2(160, 34)
	_menu_btn.text = "Voltar ao Menu"
	_menu_btn.visible = false
	_menu_btn.pressed.connect(_to_menu)
	_layer.add_child(_menu_btn)
