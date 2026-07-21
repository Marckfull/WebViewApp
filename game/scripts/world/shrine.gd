extends Interactable
## Santuário (bonfire, §3.2) — descanso: restaura vida/frascos, salva, define
## ponto de respawn e renasce os inimigos comuns. Em produção: também menu de
## atributos (gastar Ecos), fast-travel por melodia e roda da ocarina.

## Identidade do santuário para fast-travel (§3.1/§3.5).
@export var shrine_id: StringName = &"santuario"
@export var shrine_name: String = "Santuário"

func interact(_player: Node) -> void:
	var world := get_tree().get_first_node_in_group("world") as GameWorld
	if world:
		world.rest_at(global_position)
	_register()
	# Abre o menu de atributos (descansar + gastar Ecos, §3.3).
	ShrineMenu.open()

## Registra este santuário como ponto de viagem (descobbr ao descansar).
func _register() -> void:
	var cs := get_tree().current_scene
	SaveManager.state["world"]["shrines"][String(shrine_id)] = {
		"scene": cs.scene_file_path if cs else "",
		"x": global_position.x, "y": global_position.y, "name": shrine_name,
	}
