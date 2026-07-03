extends CanvasLayer
## Bolsa da Aria: lista os itens do inventário com ícone, nome,
## quantidade e descrição. Pausa o jogo enquanto aberta.

var _open := false

@onready var item_list: VBoxContainer = %ItemList
@onready var empty_label: Label = %EmptyLabel


func _ready() -> void:
	visible = false
	GameState.inventory_changed.connect(_refresh)


func _process(_delta: float) -> void:
	if Input.is_action_just_pressed("inventory"):
		_toggle()


func _toggle() -> void:
	# Não abre por cima de outro modal (diálogo).
	if not _open and get_tree().paused:
		return
	_open = not _open
	visible = _open
	get_tree().paused = _open
	AudioManager.play_sfx("blip")
	if _open:
		_refresh()


func _refresh() -> void:
	if not _open:
		return
	for child in item_list.get_children():
		child.queue_free()
	empty_label.visible = GameState.inventory.is_empty()
	for id in GameState.inventory:
		item_list.add_child(_build_row(id, int(GameState.inventory[id])))


func _build_row(id: String, count: int) -> Control:
	var data := ItemDB.get_item(id)
	var row := HBoxContainer.new()
	row.add_theme_constant_override("separation", 8)
	var icon := TextureRect.new()
	icon.custom_minimum_size = Vector2(16, 16)
	icon.stretch_mode = TextureRect.STRETCH_KEEP_CENTERED
	var icon_path: String = data.get("icon", "")
	if not icon_path.is_empty():
		icon.texture = load(icon_path)
	row.add_child(icon)
	var text := VBoxContainer.new()
	text.size_flags_horizontal = Control.SIZE_EXPAND_FILL
	var title := Label.new()
	var display_name: String = data.get("name", id)
	title.text = display_name if data.get("key", false) or count == 1 \
			else "%s ×%d" % [display_name, count]
	title.add_theme_font_size_override("font_size", 10)
	title.add_theme_color_override("font_color", Color(0.95, 0.9, 0.75))
	text.add_child(title)
	var desc := Label.new()
	desc.text = data.get("desc", "")
	desc.autowrap_mode = TextServer.AUTOWRAP_WORD_SMART
	desc.add_theme_font_size_override("font_size", 8)
	desc.add_theme_color_override("font_color", Color(0.7, 0.7, 0.78))
	text.add_child(desc)
	row.add_child(text)
	return row
