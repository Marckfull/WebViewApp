extends Interactable
## GrappleGate — passagem metroidvania (§3.3): um abismo que só se atravessa com
## o item requerido (o Gancho-corda). Sem o item, mostra a dica; com ele, puxa
## Aria para o outro lado. Estado do mundo (item) desbloqueia geografia nova.

@export var required_item: StringName = &"gancho"
@export var landing: Vector2 = Vector2.ZERO   ## destino do outro lado (global)
@export var locked_dialogue: DialogueData

func interact(player: Node) -> void:
	if Inventory.has(required_item, SaveManager.state["items"]):
		if player.has_method("grapple_to"):
			player.grapple_to(landing)
	elif locked_dialogue:
		DialogueManager.start(locked_dialogue)
