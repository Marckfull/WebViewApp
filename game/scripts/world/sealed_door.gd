extends Interactable
## SealedDoor — porta selada que só se abre com a Canção do Selo (§3.1). Gating
## por SOM (ligando música↔mecânica, pilar do GDD), distinto de item/chave.
##
## Raiz = Area2D (interação/hint); filho "Blocker" (StaticBody2D) barra a passagem.

@export var seal_id: StringName = &"selo_01"
@export var locked_dialogue: DialogueData

func _ready() -> void:
	super._ready()
	add_to_group("sealed")
	var opened: Array = SaveManager.state["world"].get("seals_opened", [])
	if opened.has(String(seal_id)):
		queue_free()

## Interagir sem a canção certa só dá a dica.
func interact(_player: Node) -> void:
	if locked_dialogue:
		DialogueManager.start(locked_dialogue)

## Chamado pela Canção do Selo (OcarinaManager) — abre e persiste.
func open_seal() -> void:
	var opened: Array = SaveManager.state["world"].get("seals_opened", [])
	if not opened.has(String(seal_id)):
		opened.append(String(seal_id))
		SaveManager.state["world"]["seals_opened"] = opened
	CombatFx.spark(self)
	queue_free()
