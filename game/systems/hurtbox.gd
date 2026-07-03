class_name Hurtbox
extends Area2D
## Área que recebe dano. O dono conecta hit_received e decide a reação.

signal hit_received(hitbox: Hitbox)

var invulnerable: bool = false


func _ready() -> void:
	area_entered.connect(_on_area_entered)


func _on_area_entered(area: Area2D) -> void:
	if invulnerable:
		return
	if area is Hitbox:
		hit_received.emit(area)
