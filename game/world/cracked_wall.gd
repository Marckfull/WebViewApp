extends StaticBody2D
## Parede rachada: só cede a uma Bomba de Eco. Fica destruída para
## sempre via flag (wall_id).

@export var wall_id := ""


func _ready() -> void:
	if not wall_id.is_empty() \
			and GameState.flags.get("parede_" + wall_id, false):
		queue_free()


func shatter() -> void:
	if not wall_id.is_empty():
		GameState.flags["parede_" + wall_id] = true
	FX.spawn_hit(global_position, Color(0.75, 0.75, 0.85))
	AudioManager.play_sfx("hit")
	GameEvents.notify("A parede cede!")
	queue_free()
