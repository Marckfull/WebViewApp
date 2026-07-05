extends Area2D
## Recurso coletável no mundo (minério, erva...). Renasce quando a
## jogadora descansa no santuário; itens-chave (one_time) não renascem
## e ficam gravados nas flags.

@export var item_id := "minerio_eco"
@export var amount := 1
@export var one_time := false
## Só aparece/coleta com a Lente da Verdade na bolsa (segredo revelado).
@export var requires_lens := false

var _collected := false

@onready var sprite: Sprite2D = $Sprite


func _ready() -> void:
	if one_time and GameState.flags.get("coletado_" + item_id, false):
		queue_free()
		return
	var icon: String = ItemDB.get_item(item_id).get("icon", "")
	if not icon.is_empty():
		sprite.texture = load(icon)
	body_entered.connect(_on_body_entered)
	if not one_time:
		GameEvents.shrine_rested.connect(_respawn)
	if requires_lens:
		GameState.inventory_changed.connect(_update_lens)
		_update_lens()


## A Lente da Verdade revela o que o Silêncio escondeu.
func _update_lens() -> void:
	var seen := GameState.has_item("lente_verdade")
	visible = seen
	set_deferred("monitoring", seen and not _collected)


func _on_body_entered(body: Node2D) -> void:
	if _collected or not (body is Player) or not body.is_alive():
		return
	if requires_lens and not GameState.has_item("lente_verdade"):
		return
	_collected = true
	GameState.add_item(item_id, amount)
	if one_time:
		GameState.flags["coletado_" + item_id] = true
	AudioManager.play_sfx("pickup")
	var item_name: String = ItemDB.get_item(item_id).get("name", item_id)
	GameEvents.notify("Coletado: %s" % item_name)
	visible = false
	set_deferred("monitoring", false)


func _respawn() -> void:
	_collected = false
	visible = true
	set_deferred("monitoring", true)
