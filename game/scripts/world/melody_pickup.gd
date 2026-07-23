extends Interactable
## MelodyPickup — item que concede uma melodia da Ocarina (§2, §3.1). No Ato 1 é
## a Ocarina de Vidro (homenagem a OoT). Interagir registra a melodia e some.

@export var melody_id: StringName = &"cancao_do_mundo"
## Melodias extras aprendidas junto (ex.: a Ocarina ensina um repertório inicial).
@export var extra_melodies: Array[String] = []
@export var dialogue: DialogueData

func interact(_player: Node) -> void:
	var melodies: Array = SaveManager.state["world"]["melodies"]
	for m in [String(melody_id)] + extra_melodies:
		if not melodies.has(m):
			melodies.append(m)
	GameEvents.melody_played.emit(melody_id)
	if dialogue:
		DialogueManager.start(dialogue)
	queue_free()
