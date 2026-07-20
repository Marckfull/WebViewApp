class_name Hitbox
extends Area2D
## Hitbox — área que causa dano. Encosta numa Hurtbox e aplica dano/postura.
##
## Padrão idiomático de combate 2D em Godot (§2 do catálogo de recursos): o par
## Hitbox/Hurtbox via Area2D é a base de todo golpe. Habilite só durante o frame
## ativo do ataque (via AnimationPlayer) para janelas de acerto precisas.

@export var damage: float = 12.0
@export var poise_damage: float = 15.0
@export var knockback: float = 120.0

func _ready() -> void:
	monitoring = false  ## desligada por padrão; a animação de ataque a liga
	area_entered.connect(_on_area_entered)

func _on_area_entered(area: Area2D) -> void:
	if area is Hurtbox:
		(area as Hurtbox).receive_hit(self)

func activate() -> void:
	monitoring = true

func deactivate() -> void:
	monitoring = false
