class_name Health
extends Node
## Componente de vida reutilizável (jogadora, inimigos, objetos quebráveis).

signal changed(current: int, max_value: int)
signal damaged(amount: int)
signal died

@export var max_health: int = 100

var current: int


func _ready() -> void:
	current = max_health
	changed.emit(current, max_health)


func damage(amount: int) -> void:
	if current <= 0:
		return
	current = maxi(current - amount, 0)
	changed.emit(current, max_health)
	damaged.emit(amount)
	if current == 0:
		died.emit()


func heal_full() -> void:
	current = max_health
	changed.emit(current, max_health)


func is_alive() -> bool:
	return current > 0
