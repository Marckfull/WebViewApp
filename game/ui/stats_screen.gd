extends CanvasLayer
## Nível de atributos no santuário (level up souls-like): gasta Ecos
## em Vitalidade, Fôlego ou Força. Abre ao descansar.

const ATTRIBUTES := [
	{"key": "vit", "name": "Vitalidade", "effect": "+10 de vida máxima"},
	{"key": "fol", "name": "Fôlego", "effect": "+8 de vigor máximo"},
	{"key": "forca", "name": "Força", "effect": "+2 de dano da lâmina"},
]

var _open := false

@onready var attr_list: VBoxContainer = %AttrList
@onready var info_label: Label = %InfoLabel
@onready var close_button: Button = %CloseButton


func _ready() -> void:
	visible = false
	GameEvents.stats_requested.connect(_open_screen)
	close_button.pressed.connect(_close)


func _open_screen() -> void:
	if _open or get_tree().paused:
		return
	_open = true
	visible = true
	get_tree().paused = true
	_refresh()


func _close() -> void:
	_open = false
	visible = false
	get_tree().paused = false
	AudioManager.play_sfx("blip")


func _refresh() -> void:
	info_label.text = "Seus Ecos: %d · Próximo nível: %d Ecos" \
			% [GameState.echoes, GameState.attribute_cost()]
	for child in attr_list.get_children():
		child.queue_free()
	for attr in ATTRIBUTES:
		attr_list.add_child(_build_row(attr))


func _build_row(attr: Dictionary) -> Control:
	var row := HBoxContainer.new()
	row.add_theme_constant_override("separation", 8)
	var text := VBoxContainer.new()
	text.size_flags_horizontal = Control.SIZE_EXPAND_FILL
	var title := Label.new()
	title.text = "%s — nível %d" % [attr["name"], int(GameState.attributes[attr["key"]])]
	title.add_theme_font_size_override("font_size", 10)
	title.add_theme_color_override("font_color", Color(0.95, 0.9, 0.75))
	text.add_child(title)
	var desc := Label.new()
	desc.text = attr["effect"]
	desc.add_theme_font_size_override("font_size", 8)
	desc.add_theme_color_override("font_color", Color(0.7, 0.7, 0.78))
	text.add_child(desc)
	row.add_child(text)
	var raise := Button.new()
	raise.custom_minimum_size = Vector2(60, 0)
	raise.add_theme_font_size_override("font_size", 10)
	raise.text = "+"
	raise.pressed.connect(_raise.bind(attr["key"]))
	row.add_child(raise)
	return row


func _raise(key: String) -> void:
	if GameState.raise_attribute(key):
		AudioManager.play_sfx("victory")
	else:
		GameEvents.notify("Ecos insuficientes.")
	_refresh()
