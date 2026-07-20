class_name LockOnSystem
extends Node2D
## LockOnSystem — o "Z-targeting" de OoT adaptado a 2D/touch (§3.1).
##
## RISCO Nº 1 do projeto (§7): combate touch precisa ser bom. Este é o gate da
## Fase 0. Regras:
##   - Auto-lock no inimigo mais próximo dentro do alcance.
##   - Tocar num inimigo troca o alvo (no touch; tecla/botão cicla).
##   - Alvo fora de alcance ou morto -> destrava.
## O grupo "enemies" é a fonte de verdade dos alvos possíveis.

@export var max_range: float = 220.0

var current_target: Node2D = null

func _process(_delta: float) -> void:
	if current_target and not _is_valid(current_target):
		current_target = null

## Alterna o lock: se travado, destrava; senão trava no mais próximo.
func toggle() -> void:
	if current_target:
		current_target = null
	else:
		current_target = _nearest_enemy()

## Troca para um alvo específico (usado pelo toque na tela em cima do inimigo).
func set_target(target: Node2D) -> void:
	if _is_valid(target):
		current_target = target

## Cicla para o próximo alvo mais próximo (botão/gamepad).
func cycle() -> void:
	var enemies := _enemies_in_range()
	if enemies.is_empty():
		current_target = null
		return
	var idx := enemies.find(current_target)
	current_target = enemies[(idx + 1) % enemies.size()]

func _nearest_enemy() -> Node2D:
	var enemies := _enemies_in_range()
	return enemies[0] if not enemies.is_empty() else null

## Inimigos válidos dentro do alcance, ordenados do mais perto ao mais longe.
func _enemies_in_range() -> Array[Node2D]:
	var result: Array[Node2D] = []
	for e in get_tree().get_nodes_in_group("enemies"):
		if e is Node2D and _is_valid(e):
			result.append(e)
	result.sort_custom(func(a, b):
		return global_position.distance_squared_to(a.global_position) \
			< global_position.distance_squared_to(b.global_position))
	return result

func _is_valid(target: Node2D) -> bool:
	if not is_instance_valid(target):
		return false
	return global_position.distance_to(target.global_position) <= max_range
