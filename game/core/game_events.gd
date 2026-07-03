extends Node
## Event bus global — desacopla sistemas (HUD, mundo, spawners, bosses).

signal player_died(position: Vector2)
signal shrine_rested
signal notified(text: String)
signal boss_engaged(boss_name: String, max_health: int)
signal boss_health_changed(current: int)
signal boss_ended(victory: bool)
signal dialog_requested(speaker: String, lines: PackedStringArray)


func notify(text: String) -> void:
	notified.emit(text)
