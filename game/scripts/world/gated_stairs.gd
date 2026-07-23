extends Interactable
## GatedStairs — a descida ao Coração Mudo (Ato 3), travada até os 4 Santuários
## serem restaurados, isto é, até os 4 bosses das dungeons caírem (§2). Com os 4,
## atravessa; sem eles, mostra quantos faltam.

@export_file("*.tscn") var target_scene: String = ""
@export var spawn_point: Vector2 = Vector2.ZERO
@export var locked_dialogue: DialogueData

func interact(_player: Node) -> void:
	var defeated: Array = SaveManager.state["world"].get("bosses_defeated", [])
	if ActProgress.all_cleared(defeated):
		if target_scene == "":
			return
		RoomStreamer.go_to(target_scene, spawn_point)
	elif locked_dialogue:
		DialogueManager.start(locked_dialogue)
