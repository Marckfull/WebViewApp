extends Marker2D
## Ponto de spawn: instancia o inimigo e o repõe quando a jogadora
## descansa no santuário (regra souls-like).

@export var enemy_scene: PackedScene

var _instance: Node


func _ready() -> void:
	GameEvents.shrine_rested.connect(_respawn)
	_respawn()


func _respawn() -> void:
	if is_instance_valid(_instance):
		_instance.queue_free()
	_instance = enemy_scene.instantiate()
	_instance.position = position
	get_parent().add_child.call_deferred(_instance)
