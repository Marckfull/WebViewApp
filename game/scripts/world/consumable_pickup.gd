extends Interactable
## ConsumablePickup — coleta um item contável (§3.5): poção (bag "consumables")
## ou recurso de craft como minério (bag "resources"). Usa o módulo Consumables.

@export var consumable_id: StringName = &"pocao_vigor"
@export var amount: int = 1
## Coleção-alvo no save: "consumables" (poções) ou "resources" (minério, madeira...).
@export_enum("consumables", "resources") var bag: String = "consumables"
@export var dialogue: DialogueData

func interact(_player: Node) -> void:
	Consumables.grant(consumable_id, SaveManager.state[bag], amount)
	if bag == "consumables":
		GameEvents.consumable_changed.emit(
			consumable_id, Consumables.count(consumable_id, SaveManager.state["consumables"]))
	if dialogue:
		DialogueManager.start(dialogue)
	queue_free()
