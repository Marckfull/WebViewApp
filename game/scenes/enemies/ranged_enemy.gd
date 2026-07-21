class_name RangedEnemy
extends CharacterBody2D
## Ecoado Arqueiro — inimigo à distância (§3.2). Mantém distância (kita), telegrafa
## e dispara um Projectile. Combate JUSTO: o tiro tem aviso visual e viaja, dando
## tempo de rolar. Data-driven via EnemyData (usa contact_damage como dano do tiro).

enum State { IDLE, REPOSITION, TELEGRAPH, RECOVER, STAGGERED }

const PROJECTILE_SCENE := preload("res://scenes/combat/projectile.tscn")
const ENEMY_HITBOX_LAYER := 16    ## camada de hitbox dos inimigos
const PLAYER_HURTBOX_LAYER := 2   ## camada de hurtbox do jogador (alvo do tiro)

@export var data: EnemyData
@export var move_speed: float = 40.0
@export var aggro_range: float = 240.0
@export var preferred_range: float = 150.0   ## distância que tenta manter
@export var telegraph_time: float = 0.6
@export var recover_time: float = 0.9
@export var projectile_damage: float = 10.0
@export var projectile_poise: float = 6.0

@onready var health: HealthComponent = $HealthComponent
@onready var visual: Polygon2D = $Visual
@onready var hurtbox: Hurtbox = $Hurtbox

var state: State = State.IDLE
var _player: Node2D
var _timer: float = 0.0
var _base_color: Color
var _dormant: bool = false

func _ready() -> void:
	add_to_group("enemies")
	if data:
		health.max_health = data.max_health
		health.max_poise = data.max_poise
		move_speed = data.move_speed
		projectile_damage = data.contact_damage
		health.health = data.max_health
		visual.color = data.tint
	var cyc := int(SaveManager.state.get("ng_cycle", 0))
	if cyc > 0:
		health.max_health *= NewGamePlus.health_mult(cyc)
		health.health = health.max_health
		projectile_damage *= NewGamePlus.damage_mult(cyc)
	_base_color = visual.color
	health.died.connect(_on_died)
	health.poise_broken.connect(_on_poise_broken)
	hurtbox.hit_taken.connect(_on_hurt)
	GameEvents.day_time_changed.connect(_on_day_time_changed)
	_update_dormancy(SaveManager.state["world"].get("day_time", 0.0))

func _on_day_time_changed(value: float) -> void:
	_update_dormancy(value)

func _update_dormancy(day_time: float) -> void:
	_dormant = data != null and data.nocturnal and day_time < 0.5
	visible = not _dormant
	hurtbox.monitorable = not _dormant
	if _dormant:
		state = State.IDLE

func _on_hurt(_hitbox: Hitbox) -> void:
	visual.modulate = Color(4, 4, 4)
	create_tween().tween_property(visual, "modulate", Color(1, 1, 1), 0.12)
	CombatFx.spark(self)

func _physics_process(delta: float) -> void:
	if _dormant:
		velocity = Vector2.ZERO
		return
	_player = _find_player()
	match state:
		State.IDLE:
			velocity = Vector2.ZERO
			if _player and _dist_to_player() <= aggro_range:
				state = State.REPOSITION
		State.REPOSITION:
			_do_reposition()
		State.TELEGRAPH, State.RECOVER, State.STAGGERED:
			velocity = Vector2.ZERO
			_tick(delta)
	move_and_slide()

## Kita: foge se o jogador chega perto, aproxima se longe, e atira quando na faixa.
func _do_reposition() -> void:
	if _player == null:
		state = State.IDLE
		return
	var d := _dist_to_player()
	if d > aggro_range * 1.3:
		state = State.IDLE
		return
	var to_player := (_player.global_position - global_position).normalized()
	if d < preferred_range * 0.7:
		velocity = -to_player * move_speed   ## recua
	elif d > preferred_range * 1.2:
		velocity = to_player * move_speed    ## aproxima
	else:
		velocity = Vector2.ZERO
		_enter_telegraph()

func _tick(delta: float) -> void:
	_timer -= delta
	if _timer > 0.0:
		return
	match state:
		State.TELEGRAPH:
			_fire()
			state = State.RECOVER
			_timer = recover_time
		State.RECOVER:
			state = State.REPOSITION
		State.STAGGERED:
			health.recover_poise()
			state = State.REPOSITION

func _enter_telegraph() -> void:
	state = State.TELEGRAPH
	_timer = telegraph_time
	visual.color = Color(1, 1, 1)  ## aviso do tiro (§3.2)

func _fire() -> void:
	visual.color = _base_color
	if _player == null:
		return
	var dir := (_player.global_position - global_position).normalized()
	var proj := PROJECTILE_SCENE.instantiate() as Projectile
	proj.setup(dir, projectile_damage, projectile_poise, ENEMY_HITBOX_LAYER, PLAYER_HURTBOX_LAYER)
	var host := get_tree().current_scene
	if host:
		host.add_child(proj)
		proj.global_position = global_position + dir * 14.0

func is_threatening() -> bool:
	return state == State.TELEGRAPH

## Canção do Mundo / parry (§3.1/§3.2) — atordoa.
func stagger() -> void:
	state = State.STAGGERED
	_timer = 1.5
	visual.color = _base_color

func _on_poise_broken() -> void:
	stagger()

func _dist_to_player() -> float:
	return global_position.distance_to(_player.global_position) if _player else INF

func _find_player() -> Node2D:
	var players := get_tree().get_nodes_in_group("player")
	return players[0] if not players.is_empty() else null

func _on_died() -> void:
	var reward: int = data.ecos_reward if data else 18
	SaveManager.state["aria"]["ecos"] += reward
	GameEvents.ecos_changed.emit(SaveManager.state["aria"]["ecos"])
	GameEvents.enemy_defeated.emit(data.id if data else &"arqueiro", global_position)
	queue_free()
