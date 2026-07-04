extends Node2D
## Bomba de Eco: pisca, explode, fere inimigos e quebra paredes rachadas.

const FUSE := 1.0
const BREAK_RADIUS := 68.0

@onready var sprite: Sprite2D = $Sprite
@onready var hitbox_shape: CollisionShape2D = $Hitbox/CollisionShape2D


func _ready() -> void:
	var tween := create_tween().set_loops(5)
	tween.tween_property(sprite, "modulate", Color(2.2, 1.3, 1.3), 0.1)
	tween.tween_property(sprite, "modulate", Color.WHITE, 0.1)
	await get_tree().create_timer(FUSE).timeout
	_explode()


func _explode() -> void:
	AudioManager.play_sfx("explosion")
	FX.shake(8.0)
	FX.spawn_hit(global_position, Color(1.0, 0.8, 0.4))
	sprite.visible = false
	hitbox_shape.set_deferred("disabled", false)
	for node in get_tree().get_nodes_in_group("breakable"):
		if global_position.distance_to(node.global_position) <= BREAK_RADIUS:
			node.shatter()
	await get_tree().create_timer(0.15).timeout
	queue_free()
