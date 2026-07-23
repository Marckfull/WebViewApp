extends Interactable
## BreakableWall — parede rachada que só a Bomba de Eco estilhaça (§3.3). Um gating
## metroidvania diferente do gancho: remove uma barreira física (abre passagem),
## em vez de atravessar um vão. Persiste como quebrada (não volta a fechar).
##
## Raiz = Area2D (interação); filho "Blocker" (StaticBody2D) faz a colisão da parede.

@export var wall_id: StringName = &"parede_01"
@export var required_item: StringName = &"bomba"
@export var locked_dialogue: DialogueData

func _ready() -> void:
	super._ready()  # entra no grupo "interactables"
	var broken: Array = SaveManager.state["world"].get("walls_broken", [])
	if broken.has(String(wall_id)):
		queue_free()  # já estilhaçada numa visita/sessão anterior

func interact(_player: Node) -> void:
	if Inventory.has(required_item, SaveManager.state["items"]):
		var broken: Array = SaveManager.state["world"].get("walls_broken", [])
		if not broken.has(String(wall_id)):
			broken.append(String(wall_id))
			SaveManager.state["world"]["walls_broken"] = broken
			SaveManager.save_game()  # marco: autosave (§3.6)
		CombatFx.spark(self)  # pequeno estilhaço
		queue_free()
	elif locked_dialogue:
		DialogueManager.start(locked_dialogue)
