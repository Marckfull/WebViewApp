class_name Hitbox
extends Area2D
## Área que causa dano. Ativada/desativada pelo dono durante o golpe.

@export var damage: int = 10
@export var knockback: float = 140.0
## Golpes não-aparáveis (ex.: explosões) ignoram o parry da jogadora.
@export var parryable: bool = true


## Primeiro ancestral no grupo "enemies" — o dono do golpe. Usado pelo
## parry para saber quem atordoar.
func source() -> Node:
	var node: Node = get_parent()
	while node != null:
		if node.is_in_group("enemies"):
			return node
		node = node.get_parent()
	return null
