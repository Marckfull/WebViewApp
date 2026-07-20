extends Node
## GameConfig — configuração global e registro de input (autoload).
##
## Registra as ações de input programaticamente (mais robusto que editar o
## project.godot à mão) com fallback de teclado para testes no desktop e
## suporte a gamepad. O stick virtual (touch) alimenta as mesmas ações via
## Input.action_press/release — ver scripts/ui/virtual_joystick.gd.
##
## Referência de dificuldade: §3.4 do GDD (Balada / Canção / Requiem).

enum Difficulty { BALADA, CANCAO, REQUIEM }

## Multiplicadores por dificuldade: [stamina, janela_parry, mantem_ecos_ao_morrer]
const DIFFICULTY_TABLE := {
	Difficulty.BALADA:  { "stamina_mult": 1.5, "parry_window": 0.30, "keep_ecos": true },
	Difficulty.CANCAO:  { "stamina_mult": 1.0, "parry_window": 0.18, "keep_ecos": false },
	Difficulty.REQUIEM: { "stamina_mult": 0.8, "parry_window": 0.12, "keep_ecos": false },
}

var difficulty: Difficulty = Difficulty.CANCAO

func _enter_tree() -> void:
	_register_actions()

func current_rules() -> Dictionary:
	return DIFFICULTY_TABLE[difficulty]

## Registra ações caso ainda não existam. Teclado = fallback de desenvolvimento;
## no Android o stick virtual e os botões touch disparam as mesmas ações.
func _register_actions() -> void:
	_add_action("move_left",  KEY_A, JOY_AXIS_LEFT_X, -1.0)
	_add_action("move_right", KEY_D, JOY_AXIS_LEFT_X, 1.0)
	_add_action("move_up",    KEY_W, JOY_AXIS_LEFT_Y, -1.0)
	_add_action("move_down",  KEY_S, JOY_AXIS_LEFT_Y, 1.0)
	_add_key_action("attack",  KEY_J, JOY_BUTTON_X)
	_add_key_action("dodge",   KEY_SPACE, JOY_BUTTON_A)
	_add_key_action("interact", KEY_E, JOY_BUTTON_Y)
	_add_key_action("guard",   KEY_K, JOY_BUTTON_RIGHT_SHOULDER)
	_add_key_action("lock_on", KEY_Q, JOY_BUTTON_LEFT_SHOULDER)
	_add_key_action("ocarina", KEY_F, JOY_BUTTON_B)

func _add_action(action_name: StringName, key: Key, axis: int, axis_value: float) -> void:
	if InputMap.has_action(action_name):
		return
	InputMap.add_action(action_name)
	var k := InputEventKey.new()
	k.physical_keycode = key
	InputMap.action_add_event(action_name, k)
	var motion := InputEventJoypadMotion.new()
	motion.axis = axis
	motion.axis_value = axis_value
	InputMap.action_add_event(action_name, motion)

func _add_key_action(action_name: StringName, key: Key, button: int) -> void:
	if InputMap.has_action(action_name):
		return
	InputMap.add_action(action_name)
	var k := InputEventKey.new()
	k.physical_keycode = key
	InputMap.action_add_event(action_name, k)
	var b := InputEventJoypadButton.new()
	b.button_index = button
	InputMap.action_add_event(action_name, b)
