extends Control
## Prólogo — narração de abertura (§2, Ato 1). Mostrado ao começar um Novo Jogo,
## antes da Cripta. Diegético e curto (nada de paredes de texto). Avança com
## toque / Interagir / Atacar.

const GAME := "res://scenes/world/vila_pedra_alva.tscn"

const PAGES := [
	"Séculos atrás, cinco Guardiãs do Eco ouviam a Canção do Mundo —\na melodia que mantinha a realidade inteira.",
	"Quando a última Guardiã se calou, o Silêncio começou:\numa névoa que apaga sons, cores e nomes.",
	"Em Pedra-Alva, uma jovem cartógrafa guarda mapas de um mundo\nque encolhe um pouco a cada noite.",
	"Aria é surda de um ouvido desde criança.\nSempre chamaram isso de defeito.",
	"Esta noite, a névoa sobe mais cedo. Aria volta para casa,\npara a irmã, Lys.",
	"Pedra-Alva, ao anoitecer. O que vem agora, Aria vai viver —\nnão só ouvir.",
]

var _text: Label
var _hint: Label
var _idx: int = 0

func _ready() -> void:
	set_anchors_preset(Control.PRESET_FULL_RECT)
	get_tree().paused = false
	GameConfig.gameplay_locked = false
	_build()
	_show_page()

func _build() -> void:
	var bg := ColorRect.new()
	bg.color = Color(0.03, 0.04, 0.06)
	bg.set_anchors_preset(Control.PRESET_FULL_RECT)
	bg.mouse_filter = Control.MOUSE_FILTER_IGNORE
	add_child(bg)

	_text = Label.new()
	_text.set_anchors_preset(Control.PRESET_CENTER)
	_text.position = Vector2(-260, -40)
	_text.custom_minimum_size = Vector2(520, 0)
	_text.horizontal_alignment = HORIZONTAL_ALIGNMENT_CENTER
	_text.autowrap_mode = TextServer.AUTOWRAP_WORD_SMART
	_text.add_theme_color_override("font_color", Color(0.85, 0.9, 1.0))
	add_child(_text)

	_hint = Label.new()
	_hint.set_anchors_preset(Control.PRESET_CENTER)
	_hint.position = Vector2(-260, 70)
	_hint.custom_minimum_size = Vector2(520, 0)
	_hint.horizontal_alignment = HORIZONTAL_ALIGNMENT_CENTER
	_hint.add_theme_color_override("font_color", Color(0.5, 0.55, 0.65))
	_hint.text = "toque para continuar"
	add_child(_hint)

func _show_page() -> void:
	_text.text = PAGES[_idx]
	_text.modulate.a = 0.0
	create_tween().tween_property(_text, "modulate:a", 1.0, 0.6)

func _advance() -> void:
	_idx += 1
	if _idx >= PAGES.size():
		get_tree().change_scene_to_file(GAME)
	else:
		_show_page()

func _input(event: InputEvent) -> void:
	var go := event.is_action_pressed("interact") or event.is_action_pressed("attack")
	if event is InputEventScreenTouch and event.pressed:
		go = true
	if go:
		get_viewport().set_input_as_handled()
		_advance()
