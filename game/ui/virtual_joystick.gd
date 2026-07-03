extends Control
## Joystick virtual flutuante (metade esquerda da tela). Injeta as ações
## de movimento via Input.action_press com força analógica.

const RADIUS := 42.0
const DEAD_ZONE := 0.15
const ACTIONS := ["move_right", "move_left", "move_down", "move_up"]

var _touch_index := -1
var _origin := Vector2.ZERO
var _vector := Vector2.ZERO


func _ready() -> void:
	set_process_input(DisplayServer.is_touchscreen_available())


func _input(event: InputEvent) -> void:
	if event is InputEventScreenTouch:
		if event.pressed and _touch_index == -1 \
				and get_global_rect().has_point(event.position):
			_touch_index = event.index
			_origin = event.position
			_vector = Vector2.ZERO
			queue_redraw()
		elif not event.pressed and event.index == _touch_index:
			_release()
	elif event is InputEventScreenDrag and event.index == _touch_index:
		_vector = (event.position - _origin).limit_length(RADIUS) / RADIUS
		if _vector.length() < DEAD_ZONE:
			_vector = Vector2.ZERO
		_apply()
		queue_redraw()


func _apply() -> void:
	Input.action_press("move_right", maxf(_vector.x, 0.0))
	Input.action_press("move_left", maxf(-_vector.x, 0.0))
	Input.action_press("move_down", maxf(_vector.y, 0.0))
	Input.action_press("move_up", maxf(-_vector.y, 0.0))


func _release() -> void:
	_touch_index = -1
	_vector = Vector2.ZERO
	for action in ACTIONS:
		Input.action_release(action)
	queue_redraw()


func _draw() -> void:
	if _touch_index == -1:
		return
	var center := _origin - get_global_position()
	draw_circle(center, RADIUS, Color(1, 1, 1, 0.07))
	draw_arc(center, RADIUS, 0, TAU, 32, Color(1, 1, 1, 0.25), 1.5)
	draw_circle(center + _vector * (RADIUS - 12.0), 12.0, Color(1, 1, 1, 0.4))
