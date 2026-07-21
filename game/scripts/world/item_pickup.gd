extends Interactable
## ItemPickup — coleta um item-chave ou memória (§3.3/§2). Data-driven pela
## coleção-alvo no save. Ao pegar o Gancho, novas passagens ficam acessíveis.

@export var item_id: StringName = &""
## Coleção no save: "items" (itens-chave) ou "memories" (Memórias Perdidas).
@export_enum("items", "memories") var collection: String = "items"
## Se definido, o item fica INVISÍVEL/intocável até Aria ter este item-chave
## (ex.: a Lente da Verdade revela o oculto, §3.3).
@export var requires_to_reveal: StringName = &""
@export var dialogue: DialogueData

func _ready() -> void:
	super._ready()  # entra no grupo "interactables"
	if requires_to_reveal != &"":
		GameEvents.item_obtained.connect(func(_id): _apply_reveal())
		_apply_reveal()

func _apply_reveal() -> void:
	var revealed := Inventory.has(requires_to_reveal, SaveManager.state["items"])
	visible = revealed
	monitorable = revealed  # oculto = não interagível

func interact(_player: Node) -> void:
	var target: Array = SaveManager.state[collection]
	if Inventory.grant(item_id, target):
		GameEvents.item_obtained.emit(item_id)
		SaveManager.save_game()  ## marco: autosave (§3.6)
	if dialogue:
		DialogueManager.start(dialogue)
	queue_free()
