extends Interactable
## Santuário (bonfire, §3.2) — descanso: restaura vida/frascos, salva, define
## ponto de respawn e renasce os inimigos comuns. Em produção: também menu de
## atributos (gastar Ecos), fast-travel por melodia e roda da ocarina.

func interact(_player: Node) -> void:
	var world := get_tree().get_first_node_in_group("world") as GameWorld
	if world:
		world.rest_at(global_position)
	# Abre o menu de atributos (descansar + gastar Ecos, §3.3).
	ShrineMenu.open()
