extends CanvasLayer
## HUD: barras de vida/stamina, contador de Ecos e mensagens de evento.

var _msg_tween: Tween

@onready var hp_bar: ProgressBar = %HpBar
@onready var stamina_bar: ProgressBar = %StaminaBar
@onready var echo_label: Label = %EchoLabel
@onready var message_label: Label = %MessageLabel


func _ready() -> void:
	var player := get_tree().get_first_node_in_group("player") as Player
	if player:
		player.health.changed.connect(_on_hp_changed)
		player.stamina.changed.connect(_on_stamina_changed)
		_on_hp_changed(player.health.current, player.health.max_health)
		_on_stamina_changed(player.stamina.current, player.stamina.max_stamina)
	GameState.echoes_changed.connect(_on_echoes_changed)
	_on_echoes_changed(GameState.echoes)
	GameEvents.notified.connect(show_message)


func _on_hp_changed(current: int, max_value: int) -> void:
	hp_bar.max_value = max_value
	hp_bar.value = current


func _on_stamina_changed(current: float, max_value: float) -> void:
	stamina_bar.max_value = max_value
	stamina_bar.value = current


func _on_echoes_changed(amount: int) -> void:
	echo_label.text = "Ecos: %d" % amount


func show_message(text: String) -> void:
	message_label.text = text
	message_label.modulate.a = 1.0
	message_label.visible = true
	if _msg_tween:
		_msg_tween.kill()
	_msg_tween = create_tween()
	_msg_tween.tween_interval(1.8)
	_msg_tween.tween_property(message_label, "modulate:a", 0.0, 0.6)
