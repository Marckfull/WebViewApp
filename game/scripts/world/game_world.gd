class_name GameWorld
extends Node2D
## GameWorld — orquestra o loop souls no nível (§3.2): ponto de respawn nos
## santuários, "morte com peso" (drop de Ecos recuperável no local), e renascimento
## dos inimigos comuns ao descansar. Nível herda desta cena/base e povoa os nós.
##
## Espera um nó "Player" no grupo "player" e inimigos no grupo "enemies".

@export var respawn_delay: float = 1.5
@export var eco_drop_scene: PackedScene = preload("res://scenes/world/eco_drop.tscn")
## Limites da câmera = fronteiras da sala (a câmera não mostra além das paredes).
@export var room_bounds: Rect2 = Rect2(0, 0, 800, 480)

var _player: Player
var _respawn_point: Vector2
var _enemy_spawns: Array[Dictionary] = []
var _active_drop: Node = null

func _ready() -> void:
	add_to_group("world")
	_player = get_tree().get_first_node_in_group("player") as Player
	if _player:
		# Chegou por uma porta? Posiciona na entrada correspondente (§3.6).
		if GameConfig.has_next_spawn:
			_player.global_position = GameConfig.next_spawn
			GameConfig.has_next_spawn = false
		_respawn_point = _player.global_position
		_apply_camera_limits()
	_record_enemy_spawns()
	_restore_drop_from_save()
	_stream_neighbors()
	GameEvents.player_died.connect(_on_player_died)

## Pré-carrega as salas vizinhas (alvos das portas) como chunks (§6.2).
func _stream_neighbors() -> void:
	var neighbors: Array = []
	for node in get_children():
		if "target_scene" in node and String(node.target_scene) != "":
			neighbors.append(String(node.target_scene))
	RoomStreamer.enter(scene_file_path, neighbors)

## Recria o drop de Ecos salvo — só na sala onde a morte ocorreu (a cena é
## registrada no drop para não reaparecer na ala errada).
func _restore_drop_from_save() -> void:
	var d: Variant = SaveManager.state["world"].get("eco_drop", {})
	if d is Dictionary and d.has("amount") and d.has("position"):
		if d.get("scene", "") != scene_file_path:
			return
		var pos := Vector2(d["position"][0], d["position"][1])
		_active_drop = _make_drop(int(d["amount"]), pos)

func _apply_camera_limits() -> void:
	var cam := _player.get_node_or_null("Camera2D") as Camera2D
	if cam == null:
		return
	cam.limit_left = int(room_bounds.position.x)
	cam.limit_top = int(room_bounds.position.y)
	cam.limit_right = int(room_bounds.position.x + room_bounds.size.x)
	cam.limit_bottom = int(room_bounds.position.y + room_bounds.size.y)

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
	SaveManager.state["world"]["eco_drop"] = {}
	SaveManager.save_game()  # recuperou os Ecos: persiste o estado limpo

func _record_enemy_spawns() -> void:
	for e in get_tree().get_nodes_in_group("enemies"):
		# Bosses não renascem ao descansar — são encontros únicos (§3.2).
		if e.is_in_group("boss"):
			continue
		if e is Node2D:
			var entry := {
				"scene": (e as Node).scene_file_path,
				"position": (e as Node2D).global_position,
			}
			# Preserva o EnemyData (ex.: variante noturna) para o respawn ser fiel.
			if "data" in e and e.data != null:
				entry["data"] = e.data.resource_path
			_enemy_spawns.append(entry)

func _on_player_died(death_position: Vector2, ecos_dropped: int) -> void:
	_spawn_drop(death_position, ecos_dropped)
	SaveManager.save_game()  # autosave: a morte é um marco (§3.6)
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
	_active_drop = _make_drop(amount, pos)
	SaveManager.state["world"]["eco_drop"] = {
		"amount": amount, "position": [pos.x, pos.y], "scene": scene_file_path,
	}

func _make_drop(amount: int, pos: Vector2) -> Node:
	var drop := eco_drop_scene.instantiate()
	drop.amount = amount
	add_child(drop)
	(drop as Node2D).global_position = pos
	return drop

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
		if spawn.has("data") and spawn["data"] != "":
			enemy.data = load(spawn["data"])  ## reaplica antes do _ready
		add_child(enemy)
		(enemy as Node2D).global_position = spawn["position"]
