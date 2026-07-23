extends Node
## CombatFx — spawner central de efeitos de combate (§5). Instancia partículas
## efêmeras na cena atual, na posição de um nó. Reutilizável (faíscas, poeira...).

const SPARK := preload("res://scenes/world/hit_spark.tscn")

func spark(at_node: Node2D) -> void:
	if at_node == null:
		return
	var host: Node = at_node.get_tree().current_scene
	if host == null:
		host = at_node.get_parent()
	if host == null:
		return
	var fx := SPARK.instantiate()
	host.add_child(fx)
	(fx as Node2D).global_position = at_node.global_position
