extends Interactable
## ItemPickup — coleta um item-chave ou memória (§3.3/§2). Data-driven pela
## coleção-alvo no save. Ao pegar o Gancho, novas passagens ficam acessíveis.

@export var item_id: StringName = &""
## Coleção no save: "items" (itens-chave) ou "memories" (Memórias Perdidas).
@export_enum("items", "memories") var collection: String = "items"
@export var dialogue: DialogueData

func interact(_player: Node) -> void:
	var target: Array = SaveManager.state[collection]
	if Inventory.grant(item_id, target):
		GameEvents.item_obtained.emit(item_id)
		SaveManager.save_game()  ## marco: autosave (§3.6)
	if dialogue:
		DialogueManager.start(dialogue)
	queue_free()
