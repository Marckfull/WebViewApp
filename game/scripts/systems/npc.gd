extends Interactable
## NPC — personagem que dispara um diálogo ao interagir (§2, §3.5).
## Data-driven: a fala vem de um DialogueData (.tres). Ex.: o mercador Corvo.

@export var dialogue: DialogueData

func interact(_player: Node) -> void:
	if dialogue:
		DialogueManager.start(dialogue)
