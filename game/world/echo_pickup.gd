class_name EchoPickup
extends Node2D
## Fragmento de Eco: flutua, é atraído pela jogadora e somado ao total.
## Também usado como o "drop de almas" no local da morte.

const ATTRACT_RANGE := 56.0
const COLLECT_RANGE := 10.0
const SPEED := 150.0

@export var amount := 10

var _time := 0.0

@onready var visual: Polygon2D = $Visual


func _process(delta: float) -> void:
	_time += delta
	visual.position.y = sin(_time * 4.0) * 2.0
	var player := get_tree().get_first_node_in_group("player") as Player
	if player == null or not player.is_alive():
		return
	var dist := global_position.distance_to(player.global_position)
	if dist <= COLLECT_RANGE:
		GameState.add_echoes(amount)
		queue_free()
	elif dist <= ATTRACT_RANGE:
		global_position = global_position.move_toward(
				player.global_position, SPEED * delta)
