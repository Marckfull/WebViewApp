extends Interactable
## MelodyPickup — item que concede uma melodia da Ocarina (§2, §3.1). No Ato 1 é
## a Ocarina de Vidro (homenagem a OoT). Interagir registra a melodia e some.

@export var melody_id: StringName = &"cancao_do_mundo"
@export var dialogue: DialogueData

func interact(_player: Node) -> void:
	var melodies: Array = SaveManager.state["world"]["melodies"]
	if not melodies.has(String(melody_id)):
		melodies.append(String(melody_id))
	GameEvents.melody_played.emit(melody_id)
	if dialogue:
		DialogueManager.start(dialogue)
	queue_free()
