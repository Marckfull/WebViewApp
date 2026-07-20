class_name GameWorld
extends Node2D
## GameWorld — orquestra o loop souls no nível (§3.2): ponto de respawn nos
## santuários, "morte com peso" (drop de Ecos recuperável no local), e renascimento
## dos inimigos comuns ao descansar. Nível herda desta cena/base e povoa os nós.
##
## Espera um nó "Player" no grupo "player" e inimigos no grupo "enemies".

@export var respawn_delay: float = 1.5
@export var eco_drop_scene: PackedScene = preload("res://scenes/world/eco_drop.tscn")

var _player: Player
var _respawn_point: Vector2
var _enemy_spawns: Array[Dictionary] = []
var _active_drop: Node = null

func _ready() -> void:
	add_to_group("world")
	_player = get_tree().get_first_node_in_group("player") as Player
	if _player:
		_respawn_point = _player.global_position
	_record_enemy_spawns()
	GameEvents.player_died.connect(_on_player_died)

## Chamado pelo Santuário ao descansar (§3.2): salva, cura, renasce inimigos.
func rest_at(point: Vector2) -> void:
	_respawn_point = point
	if _player:
		_player.full_restore()
	_respawn_enemies()
	SaveManager.state["aria"]["position"] = [_respawn_point.x, _respawn_point.y]
	SaveManager.save_game()
	GameEvents.rested_at_shrine.emit(&"shrine")

func clear_active_drop() -> void:
	_active_drop = null

func _record_enemy_spawns() -> void:
	for e in get_tree().get_nodes_in_group("enemies"):
		# Bosses não renascem ao descansar — são encontros únicos (§3.2).
		if e.is_in_group("boss"):
			continue
		if e is Node2D:
			_enemy_spawns.append({
				"scene": (e as Node).scene_file_path,
				"position": (e as Node2D).global_position,
			})

func _on_player_died(death_position: Vector2, ecos_dropped: int) -> void:
	_spawn_drop(death_position, ecos_dropped)
	await get_tree().create_timer(respawn_delay).timeout
	_respawn_enemies()
	if _player:
		_player.global_position = _respawn_point
		_player.revive()

## Só existe um drop por vez: morrer de novo antes de recuperar apaga o anterior (§3.2).
func _spawn_drop(pos: Vector2, amount: int) -> void:
	if amount <= 0:
		return
	if is_instance_valid(_active_drop):
		_active_drop.queue_free()
	var drop := eco_drop_scene.instantiate()
	drop.amount = amount
	add_child(drop)
	(drop as Node2D).global_position = pos
	_active_drop = drop

func _respawn_enemies() -> void:
	for e in get_tree().get_nodes_in_group("enemies"):
		# Preserva o boss vivo: descansar não o remove nem o ressuscita.
		if e.is_in_group("boss"):
			continue
		e.queue_free()
	for spawn in _enemy_spawns:
		if spawn["scene"] == "":
			continue
		var scene: PackedScene = load(spawn["scene"])
		var enemy := scene.instantiate()
		add_child(enemy)
		(enemy as Node2D).global_position = spawn["position"]
