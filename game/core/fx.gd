extends Node
## Efeitos de "game feel": hit-stop, screen shake e partículas de impacto.

const HIT_PARTICLES := preload("res://world/hit_particles.tscn")
const SHAKE_DECAY := 26.0

var _shake := 0.0
var _shaking := false


func _process(delta: float) -> void:
	var camera := get_viewport().get_camera_2d()
	if camera == null:
		return
	if _shake > 0.0:
		_shaking = true
		_shake = maxf(_shake - SHAKE_DECAY * delta, 0.0)
		camera.offset = Vector2(
				randf_range(-_shake, _shake), randf_range(-_shake, _shake))
	elif _shaking:
		_shaking = false
		camera.offset = Vector2.ZERO


func shake(strength: float) -> void:
	_shake = maxf(_shake, strength)


## Congela o tempo por um instante no impacto (hit-stop).
func hit_stop(duration := 0.05, time_scale := 0.1) -> void:
	if Engine.time_scale < 1.0:
		return
	Engine.time_scale = time_scale
	await get_tree().create_timer(duration, true, false, true).timeout
	Engine.time_scale = 1.0


func spawn_hit(at: Vector2, color := Color(1.0, 0.92, 0.6)) -> void:
	var scene := get_tree().current_scene
	if scene == null:
		return
	var particles := HIT_PARTICLES.instantiate()
	particles.position = at
	particles.modulate = color
	scene.add_child.call_deferred(particles)
