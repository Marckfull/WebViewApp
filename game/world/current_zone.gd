extends Area2D
## Corrente de sal da Necrópole: sem as Botas de Corrente, varre a Aria
## para trás e fere aos poucos. Com as Botas, atravessa em segurança.

const TICK := 0.5

@export var flow := Vector2(0, -220)
@export var damage := 8

var _player: Player
var _tick := 0.0


func _ready() -> void:
	body_entered.connect(_on_body_entered)
	body_exited.connect(_on_body_exited)


func _physics_process(delta: float) -> void:
	if _player == null or not _player.is_alive():
		return
	if GameState.has_item("botas_corrente"):
		return
	_player.push(flow)
	_tick -= delta
	if _tick <= 0.0:
		_tick = TICK
		_player.take_environmental_damage(damage)


func _on_body_entered(body: Node2D) -> void:
	if body is Player:
		_player = body
		_tick = 0.0


func _on_body_exited(body: Node2D) -> void:
	if body == _player:
		_player = null
