extends Node
## OcarinaManager — roda de melodias da Ocarina (autoload, §3.1/§6).
##
## Abre com a ação "ocarina" (se Aria já tiver a Ocarina). Mini-teclado de 5 notas:
## toque a sequência; ao casar com uma melodia conhecida, o efeito dispara.
## A Canção do Mundo acalma os Ecoados (stagger). Data-driven via MelodyData.

const NOTE_NAMES := ["Dó", "Ré", "Mi", "Fá", "Sol"]
const MELODY_PATHS := ["res://data/melodies/cancao_do_mundo.tres"]

var _melodies: Array[MelodyData] = []
var _entered: Array[int] = []
var _open: bool = false

var _layer: CanvasLayer
var _seq_label: Label
var _title_label: Label

func _ready() -> void:
	for p in MELODY_PATHS:
		var m := load(p) as MelodyData
		if m:
			_melodies.append(m)
	_build_ui()

func _input(event: InputEvent) -> void:
	if not event.is_action_pressed("ocarina"):
		return
	toggle()
	get_viewport().set_input_as_handled()

## Público: teclado/gamepad chamam via _input; o botão touch do HUD chama direto
## (Input.action_press não gera evento para o _input).
func toggle() -> void:
	if _open:
		_close()
	elif not GameConfig.gameplay_locked and _knows_any():
		_open_wheel()

## Melodias que Aria realmente aprendeu (pegou a Ocarina / a melodia).
func _known_melodies() -> Array:
	var learned: Array = SaveManager.state["world"]["melodies"]
	var out: Array = []
	for m in _melodies:
		if learned.has(String(m.id)):
			out.append(m)
	return out

func _knows_any() -> bool:
	return not _known_melodies().is_empty()

func _open_wheel() -> void:
	_open = true
	_entered = []
	GameConfig.gameplay_locked = true
	_update_labels()
	_layer.visible = true

func _close() -> void:
	_open = false
	_layer.visible = false
	GameConfig.gameplay_locked = false

func _press_note(index: int) -> void:
	if not _open:
		return
	_entered.append(index)
	_update_labels()
	_evaluate()

## Casa a sequência: toca no acerto exato; reinicia se deixar de ser prefixo de
## qualquer melodia conhecida. Lógica pura em MelodyMatcher (testável).
func _evaluate() -> void:
	var known := _known_melodies()
	var hit := MelodyMatcher.exact_match(_entered, known)
	if hit:
		_play(hit)
		return
	if not MelodyMatcher.any_prefix(_entered, known):
		_entered = []
		_update_labels()

func _play(melody: MelodyData) -> void:
	GameEvents.melody_played.emit(melody.id)
	_title_label.text = "♪ %s ♪" % melody.display_name
	_apply_effect(melody)
	await get_tree().create_timer(0.5).timeout
	_close()

func _apply_effect(melody: MelodyData) -> void:
	match melody.effect:
		MelodyData.Effect.CALM_ENEMIES:
			for e in get_tree().get_nodes_in_group("enemies"):
				if e.has_method("stagger"):
					e.stagger()
		MelodyData.Effect.DAY_NIGHT:
			var t: float = SaveManager.state["world"]["day_time"]
			SaveManager.state["world"]["day_time"] = 0.5 if t < 0.5 else 0.0
		_:
			pass

func _update_labels() -> void:
	if _entered.is_empty():
		_seq_label.text = "—"
	else:
		var s := ""
		for i in _entered:
			s += NOTE_NAMES[i] + " "
		_seq_label.text = s.strip_edges()

func _build_ui() -> void:
	_layer = CanvasLayer.new()
	_layer.layer = 11
	_layer.visible = false
	add_child(_layer)

	var panel := Panel.new()
	panel.set_anchors_preset(Control.PRESET_CENTER)
	panel.custom_minimum_size = Vector2(360, 150)
	panel.size = Vector2(360, 150)
	panel.position = Vector2(-180, -75)
	_layer.add_child(panel)

	_title_label = Label.new()
	_title_label.position = Vector2(12, 8)
	_title_label.text = "Ocarina de Vidro"
	_title_label.add_theme_color_override("font_color", Color(0.4, 0.85, 1))
	panel.add_child(_title_label)

	_seq_label = Label.new()
	_seq_label.position = Vector2(12, 34)
	_seq_label.text = "—"
	panel.add_child(_seq_label)

	var row := HBoxContainer.new()
	row.position = Vector2(12, 64)
	row.add_theme_constant_override("separation", 8)
	panel.add_child(row)
	for i in NOTE_NAMES.size():
		var b := Button.new()
		b.text = NOTE_NAMES[i]
		b.custom_minimum_size = Vector2(58, 48)
		var idx := i
		b.pressed.connect(func(): _press_note(idx))
		row.add_child(b)

	var close_btn := Button.new()
	close_btn.text = "Fechar"
	close_btn.position = Vector2(12, 118)
	close_btn.pressed.connect(_close)
	panel.add_child(close_btn)
