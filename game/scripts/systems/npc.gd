extends Interactable
## NPC — personagem que dispara um diálogo ao interagir (§2, §3.5).
## Data-driven: a fala vem de um DialogueData (.tres). Ex.: o mercador Corvo.

@export var dialogue: DialogueData
## Fala alternativa à noite (§3.5). Se vazia, usa a fala normal a qualquer hora.
@export var night_dialogue: DialogueData

func interact(_player: Node) -> void:
	var d := dialogue
	if night_dialogue and WorldTime.is_night(SaveManager.state["world"].get("day_time", 0.0)):
		d = night_dialogue
	if d:
		DialogueManager.start(d)
