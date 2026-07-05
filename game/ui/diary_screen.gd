extends CanvasLayer
## Diário de Aria: as Memórias Perdidas reunidas e o Bestiário das
## criaturas do Silêncio. Pausa o jogo enquanto aberto.

## As 12 Memórias Perdidas, na ordem em que o Diário as lista.
const MEMORY_IDS := [
	"memoria_lys", "memoria_pedra_alva", "memoria_bosque", "memoria_treino",
	"memoria_cripta", "memoria_forja", "memoria_torre", "memoria_necropole",
	"memoria_sela", "memoria_odara", "memoria_corvo", "memoria_coracao",
]

var _open := false
var _tab := "memorias"

@onready var title: Label = %Title
@onready var tab_mem: Button = %TabMem
@onready var tab_best: Button = %TabBest
@onready var list: VBoxContainer = %List


func _ready() -> void:
	visible = false
	tab_mem.pressed.connect(_show_tab.bind("memorias"))
	tab_best.pressed.connect(_show_tab.bind("bestiario"))


func _process(_delta: float) -> void:
	if Input.is_action_just_pressed("diary"):
		_toggle()


func _toggle() -> void:
	if not _open and get_tree().paused:
		return
	_open = not _open
	visible = _open
	get_tree().paused = _open
	AudioManager.play_sfx("blip")
	if _open:
		_show_tab(_tab)


func _show_tab(tab: String) -> void:
	_tab = tab
	AudioManager.play_sfx("blip")
	for child in list.get_children():
		child.queue_free()
	if tab == "memorias":
		_build_memories()
	else:
		_build_bestiary()


func _build_memories() -> void:
	var have := 0
	for id in MEMORY_IDS:
		if GameState.has_item(id):
			have += 1
	title.text = "Memórias Perdidas — %d / %d" % [have, MEMORY_IDS.size()]
	for id in MEMORY_IDS:
		var data := ItemDB.get_item(id)
		if GameState.has_item(id):
			list.add_child(_entry(data.get("name", id), data.get("desc", "")))
		else:
			list.add_child(_entry("??? — Memória não encontrada", "", true))


func _build_bestiary() -> void:
	var seen := 0
	for id in BestiaryDB.DB:
		if GameState.bestiary.has(id):
			seen += 1
	title.text = "Bestiário — %d / %d" % [seen, BestiaryDB.DB.size()]
	for id in BestiaryDB.DB:
		var data: Dictionary = BestiaryDB.DB[id]
		if GameState.bestiary.has(id):
			var kills := int(GameState.bestiary[id])
			var name_text := "%s  (derrotados: %d)" % [data["name"], kills]
			list.add_child(_entry(name_text, data["desc"]))
		else:
			list.add_child(_entry("??? — Criatura não enfrentada", "", true))


func _entry(title_text: String, desc_text: String, faded := false) -> Control:
	var box := VBoxContainer.new()
	box.add_theme_constant_override("separation", 2)
	var name_label := Label.new()
	name_label.text = title_text
	name_label.add_theme_font_size_override("font_size", 10)
	var name_color := Color(0.6, 0.6, 0.66) if faded else Color(0.95, 0.9, 0.75)
	name_label.add_theme_color_override("font_color", name_color)
	box.add_child(name_label)
	if not desc_text.is_empty():
		var desc := Label.new()
		desc.text = desc_text
		desc.autowrap_mode = TextServer.AUTOWRAP_WORD_SMART
		desc.add_theme_font_size_override("font_size", 8)
		desc.add_theme_color_override("font_color", Color(0.72, 0.72, 0.8))
		box.add_child(desc)
	return box
