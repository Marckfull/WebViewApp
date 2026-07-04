extends Area2D
## Banca do Corvo: interagir abre a loja (gastar Ecos).

var _player: Player

@onready var prompt: Label = $Prompt


func _ready() -> void:
	body_entered.connect(_on_body_entered)
	body_exited.connect(_on_body_exited)
	prompt.visible = false


func _physics_process(_delta: float) -> void:
	if _player and _player.is_alive() and Input.is_action_just_pressed("interact") \
			and not get_tree().paused:
		GameEvents.shop_requested.emit()


func _on_body_entered(body: Node2D) -> void:
	if body is Player:
		_player = body
		prompt.visible = true


func _on_body_exited(body: Node2D) -> void:
	if body == _player:
		_player = null
		prompt.visible = false
