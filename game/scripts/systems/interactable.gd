class_name Interactable
extends Area2D
## Interactable — base de tudo que responde ao botão Interagir (§3.1):
## NPCs, santuários, itens, baús, drops de Eco. O Player detecta os que estão
## dentro do alcance (via InteractionDetector) e chama interact() no mais próximo.

@export var prompt: String = "Interagir"

func _ready() -> void:
	add_to_group("interactables")

## Sobrescrito por cada tipo. `player` é o Player que interagiu.
func interact(_player: Node) -> void:
	pass
