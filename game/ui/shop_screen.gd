extends CanvasLayer
## Loja do Corvo: gasta Ecos em melhorias permanentes. Pausa o jogo.

const MAX_FLASKS := 5

var _open := false

@onready var offer_list: VBoxContainer = %OfferList
@onready var echoes_label: Label = %EchoesLabel
@onready var close_button: Button = %CloseButton


func _ready() -> void:
	visible = false
	GameEvents.shop_requested.connect(_open_shop)
	close_button.pressed.connect(_close)


func _open_shop() -> void:
	if _open or get_tree().paused:
		return
	_open = true
	visible = true
	get_tree().paused = true
	AudioManager.play_sfx("blip")
	_refresh()


func _close() -> void:
	_open = false
	visible = false
	get_tree().paused = false
	AudioManager.play_sfx("blip")


func _offers() -> Array:
	var flask_price := 300 if GameState.flasks_max <= 3 else 600
	return [
		{
			"id": "frasco_max",
			"name": "Frasco de Essência extra",
			"desc": "Aria carrega um frasco a mais (máx. %d)." % MAX_FLASKS,
			"price": flask_price,
			"sold_out": GameState.flasks_max >= MAX_FLASKS,
		},
		{
			"id": "amuleto_eco",
			"name": "Amuleto do Eco",
			"desc": "+20 de vida máxima.",
			"price": 400,
			"sold_out": GameState.has_item("amuleto_eco"),
		},
		{
			"id": "amuleto_vento",
			"name": "Amuleto do Vento",
			"desc": "+20 de vigor máximo.",
			"price": 400,
			"sold_out": GameState.has_item("amuleto_vento"),
		},
		{
			"id": "adaga",
			"name": "Adaga Dupla",
			"desc": "Arma rápida de quatro golpes. Equipe pela bolsa.",
			"price": 350,
			"sold_out": GameState.has_item("adaga"),
		},
		{
			"id": "martelo",
			"name": "Martelo da Forja",
			"desc": "Arma pesada e devastadora. Equipe pela bolsa.",
			"price": 650,
			"sold_out": GameState.has_item("martelo"),
		},
		{
			"id": "memoria_corvo",
			"name": "Memória Perdida (Corvo)",
			"desc": "\"Uma lembrança que não me serve mais. A você, talvez.\"",
			"price": 500,
			"sold_out": GameState.has_item("memoria_corvo"),
		},
	]


func _refresh() -> void:
	echoes_label.text = "Seus Ecos: %d" % GameState.echoes
	for child in offer_list.get_children():
		child.queue_free()
	for offer in _offers():
		offer_list.add_child(_build_row(offer))


func _build_row(offer: Dictionary) -> Control:
	var row := HBoxContainer.new()
	row.add_theme_constant_override("separation", 8)
	var text := VBoxContainer.new()
	text.size_flags_horizontal = Control.SIZE_EXPAND_FILL
	var title := Label.new()
	title.text = offer["name"]
	title.add_theme_font_size_override("font_size", 10)
	title.add_theme_color_override("font_color", Color(0.95, 0.9, 0.75))
	text.add_child(title)
	var desc := Label.new()
	desc.text = offer["desc"]
	desc.autowrap_mode = TextServer.AUTOWRAP_WORD_SMART
	desc.add_theme_font_size_override("font_size", 8)
	desc.add_theme_color_override("font_color", Color(0.7, 0.7, 0.78))
	text.add_child(desc)
	row.add_child(text)
	var buy := Button.new()
	buy.custom_minimum_size = Vector2(86, 0)
	buy.add_theme_font_size_override("font_size", 9)
	if offer["sold_out"]:
		buy.text = "Esgotado"
		buy.disabled = true
	else:
		buy.text = "%d Ecos" % offer["price"]
		buy.pressed.connect(_buy.bind(offer))
	row.add_child(buy)
	return row


func _buy(offer: Dictionary) -> void:
	if not GameState.spend_echoes(offer["price"]):
		GameEvents.notify("Ecos insuficientes. Volte quando tilintar.")
		return
	match offer["id"]:
		"frasco_max":
			GameState.flasks_max += 1
			GameState.refill_flasks()
		"amuleto_eco", "amuleto_vento", "memoria_corvo", "adaga", "martelo":
			GameState.add_item(offer["id"])
	AudioManager.play_sfx("pickup")
	GameEvents.notify("Comprado: %s" % offer["name"])
	_refresh()
