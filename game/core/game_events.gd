extends Node
## Event bus global — desacopla sistemas (HUD, mundo, spawners).

signal player_died(position: Vector2)
signal shrine_rested
signal notified(text: String)


func notify(text: String) -> void:
	notified.emit(text)
