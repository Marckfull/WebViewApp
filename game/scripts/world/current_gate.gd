extends Interactable
## CurrentGate — corrente de sal que empurra Aria de volta (§3.3). Só as Botas de
## Corrente permitem ancorar e atravessar. É o gating metroidvania da Necrópole de
## Sal: sem as Botas, a dica; com elas, Aria fixa os pés e cruza para o outro lado.

@export var required_item: StringName = &"botas"
@export var landing: Vector2 = Vector2.ZERO   ## destino do outro lado (global)
@export var locked_dialogue: DialogueData

func interact(player: Node) -> void:
	if Inventory.has(required_item, SaveManager.state["items"]):
		if player.has_method("grapple_to"):
			player.grapple_to(landing)
	elif locked_dialogue:
		DialogueManager.start(locked_dialogue)
