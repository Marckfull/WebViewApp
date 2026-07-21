extends Interactable
## ConsumablePickup — coleta um consumível contável (§3.5), ex. Poção de Vigor.

@export var consumable_id: StringName = &"pocao_vigor"
@export var amount: int = 1
@export var dialogue: DialogueData

func interact(_player: Node) -> void:
	Consumables.grant(consumable_id, SaveManager.state["consumables"], amount)
	GameEvents.consumable_changed.emit(
		consumable_id, Consumables.count(consumable_id, SaveManager.state["consumables"]))
	if dialogue:
		DialogueManager.start(dialogue)
	queue_free()
