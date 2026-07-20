extends Interactable
## EcoDrop — os Ecos deixados no local da morte (§3.2). Interagir recupera tudo.
## Uma chance de recuperar; morrer de novo antes apaga este drop (ver GameWorld).

var amount: int = 0

func interact(_player: Node) -> void:
	SaveManager.state["aria"]["ecos"] += amount
	GameEvents.ecos_changed.emit(SaveManager.state["aria"]["ecos"])
	var world := get_tree().get_first_node_in_group("world") as GameWorld
	if world:
		world.clear_active_drop()
	queue_free()
