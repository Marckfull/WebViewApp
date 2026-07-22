extends DummyEnemy
## Ecoado Divisor — inimigo especial (§3.2). Reusa a IA melee do DummyEnemy, mas
## ao MORRER se parte em dois fragmentos menores (mesma cena, is_shard=true, com
## EnemyData mais fraco). Os fragmentos não se partem de novo. Mecânica de
## "pressão crescente": mate rápido ou seja cercado.

const SELF_PATH := "res://scenes/enemies/splitter_enemy.tscn"

@export var is_shard: bool = false
@export var shard_data: EnemyData          ## EnemyData dado aos fragmentos
@export var shard_count: int = 2

func _ready() -> void:
	super._ready()
	if is_shard:
		scale = Vector2(0.7, 0.7)  ## fragmentos são visualmente menores

func _on_died() -> void:
	if not is_shard and shard_data != null:
		_spawn_shards()
	super._on_died()  ## recompensa + enemy_defeated + queue_free

func _spawn_shards() -> void:
	var host := get_tree().current_scene
	if host == null:
		return
	var scene := load(SELF_PATH) as PackedScene  # runtime: evita ciclo script<->cena
	if scene == null:
		return
	for i in shard_count:
		var s := scene.instantiate()
		s.data = shard_data
		s.is_shard = true
		host.add_child(s)
		var angle := TAU * float(i) / float(maxi(shard_count, 1))
		(s as Node2D).global_position = global_position + Vector2(cos(angle), sin(angle)) * 18.0
