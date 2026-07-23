extends Interactable
## Alchemist — a alquimista de Pedra-Alva. Interagir abre a alquimia (§3.5).

func interact(_player: Node) -> void:
	AlchemyMenu.open()
