extends Area2D
## Corrente de vento da Torre: empurra a jogadora enquanto ela está dentro.

@export var force := Vector2(60, 0)

var _player: Player


func _ready() -> void:
	body_entered.connect(_on_body_entered)
	body_exited.connect(_on_body_exited)


func _physics_process(_delta: float) -> void:
	# As Botas de Corrente firmam a Aria contra o vento.
	if _player and _player.is_alive() and not GameState.has_item("botas_corrente"):
		_player.push(force)


func _on_body_entered(body: Node2D) -> void:
	if body is Player:
		_player = body


func _on_body_exited(body: Node2D) -> void:
	if body == _player:
		_player = null
