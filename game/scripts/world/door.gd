extends Interactable
## Door — transição entre alas da dungeon (§3.6: sessões curtas com checkpoints).
## Trocar de cena recria a sala (streaming leve, §6.2) e reposiciona Aria na
## entrada correspondente da ala de destino.

@export_file("*.tscn") var target_scene: String = ""
@export var spawn_point: Vector2 = Vector2.ZERO

func interact(_player: Node) -> void:
	if target_scene == "":
		return
	GameConfig.next_spawn = spawn_point
	GameConfig.has_next_spawn = true
	get_tree().change_scene_to_file(target_scene)
