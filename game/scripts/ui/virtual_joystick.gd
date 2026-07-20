class_name VirtualJoystick
extends Control
## VirtualJoystick — stick virtual touch que alimenta as ações de movimento (§3.1).
##
## Fundação própria e leve para a Fase 0. Para produção, considerar trocar por
## MarcoFazioRandom/Virtual-Joystick-Godot (MIT) — ver catálogo de recursos §5.
## Modo dinâmico: aparece onde o dedo toca. Alimenta move_left/right/up/down para
## que Input.get_vector() funcione igual ao teclado/gamepad.
##
## direção analógica também exposta via get_direction() para movimento 8-dir suave.

@export var radius: float = 80.0
@export var deadzone: float = 0.2

var _touch_index: int = -1
var _origin: Vector2
var _direction: Vector2 = Vector2.ZERO

func get_direction() -> Vector2:
	return _direction

func _gui_input(event: InputEvent) -> void:
	if event is InputEventScreenTouch:
		if event.pressed and _touch_index == -1:
			_touch_index = event.index
			_origin = event.position
		elif not event.pressed and event.index == _touch_index:
			_release()
	elif event is InputEventScreenDrag and event.index == _touch_index:
		_update_direction(event.position)

func _update_direction(pos: Vector2) -> void:
	var offset := pos - _origin
	if offset.length() > radius:
		offset = offset.normalized() * radius
	var vec := offset / radius
	_direction = vec if vec.length() > deadzone else Vector2.ZERO
	_feed_actions()

## Traduz a direção analógica de volta para as ações registradas, com força
## proporcional — assim get_vector() no Player enxerga o mesmo que o teclado.
func _feed_actions() -> void:
	_set_action("move_left",  maxf(-_direction.x, 0.0))
	_set_action("move_right", maxf(_direction.x, 0.0))
	_set_action("move_up",    maxf(-_direction.y, 0.0))
	_set_action("move_down",  maxf(_direction.y, 0.0))

func _set_action(action: StringName, strength: float) -> void:
	if strength > 0.0:
		Input.action_press(action, strength)
	else:
		Input.action_release(action)

func _release() -> void:
	_touch_index = -1
	_direction = Vector2.ZERO
	for a in ["move_left", "move_right", "move_up", "move_down"]:
		Input.action_release(a)
