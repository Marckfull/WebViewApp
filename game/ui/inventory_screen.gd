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
	item_list.add_child(_weapons_section())
	for id in GameState.inventory:
		item_list.add_child(_build_row(id, int(GameState.inventory[id])))


## Seção "Armas": a Espada está sempre disponível; adaga e martelo se
## conquistadas. Clicar equipa.
func _weapons_section() -> Control:
	var box := VBoxContainer.new()
	box.add_theme_constant_override("separation", 4)
	var header := Label.new()
	header.text = "Armas"
	header.add_theme_font_size_override("font_size", 9)
	header.add_theme_color_override("font_color", Color(0.7, 0.75, 0.55))
	box.add_child(header)
	for id in WeaponDB.DB:
		if id != "espada" and not GameState.has_item(id):
			continue
		box.add_child(_weapon_row(id))
	return box


func _weapon_row(id: String) -> Control:
	var data: Dictionary = WeaponDB.DB[id]
	var row := HBoxContainer.new()
	row.add_theme_constant_override("separation", 8)
	var text := VBoxContainer.new()
	text.size_flags_horizontal = Control.SIZE_EXPAND_FILL
	var title := Label.new()
	title.text = data["name"]
	title.add_theme_font_size_override("font_size", 10)
	title.add_theme_color_override("font_color", Color(0.95, 0.9, 0.75))
	text.add_child(title)
	var desc := Label.new()
	desc.text = data["desc"]
	desc.autowrap_mode = TextServer.AUTOWRAP_WORD_SMART
	desc.add_theme_font_size_override("font_size", 8)
	desc.add_theme_color_override("font_color", Color(0.7, 0.7, 0.78))
	text.add_child(desc)
	row.add_child(text)
	var button := Button.new()
	button.custom_minimum_size = Vector2(80, 0)
	button.add_theme_font_size_override("font_size", 9)
	if GameState.equipped_weapon == id:
		button.text = "Equipada"
		button.disabled = true
	else:
		button.text = "Equipar"
		button.pressed.connect(_equip.bind(id))
	row.add_child(button)
	return row


func _equip(id: String) -> void:
	GameState.equip_weapon(id)
	AudioManager.play_sfx("blip")
	GameEvents.notify("Empunhando: %s" % WeaponDB.get_weapon(id)["name"])
	_refresh()


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
