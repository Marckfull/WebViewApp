class_name Stamina
extends Node
## Stamina souls-like: ataques e esquivas gastam; regenera após breve pausa.

signal changed(current: float, max_value: float)

@export var max_stamina: float = 100.0
@export var regen_per_second: float = 38.0
@export var regen_delay: float = 0.55

var current: float
var _delay_timer: float = 0.0


func _ready() -> void:
	current = max_stamina
	changed.emit(current, max_stamina)


func _process(delta: float) -> void:
	if _delay_timer > 0.0:
		_delay_timer -= delta
	elif current < max_stamina:
		current = minf(current + regen_per_second * delta, max_stamina)
		changed.emit(current, max_stamina)


func try_spend(amount: float) -> bool:
	if current < amount:
		return false
	current -= amount
	_delay_timer = regen_delay
	changed.emit(current, max_stamina)
	return true


func refill() -> void:
	current = max_stamina
	changed.emit(current, max_stamina)
