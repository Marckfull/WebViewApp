class_name DummyEnemy
extends CharacterBody2D
## Ecoado — inimigo comum com combate JUSTO (§3.2): persegue dentro do alcance de
## aggro, telegrafa o golpe (aviso visual), ataca numa janela curta e recupera.
## Separa-se dos outros Ecoados para não empilhar. Data-driven via EnemyData.

enum State { IDLE, CHASE, TELEGRAPH, ATTACK, RECOVER, STAGGERED }

@export var data: EnemyData
@export var move_speed: float = 40.0
@export var aggro_range: float = 160.0
@export var attack_range: float = 30.0
@export var telegraph_time: float = 0.45
@export var attack_time: float = 0.18
@export var recover_time: float = 0.6
@export var separation_radius: float = 26.0
@export var separation_strength: float = 30.0

@onready var health: HealthComponent = $HealthComponent
@onready var attack_hitbox: Hitbox = $ContactHitbox
@onready var visual: Polygon2D = $Visual

var state: State = State.IDLE
var _player: Node2D
var _timer: float = 0.0
var _base_color: Color

func _ready() -> void:
	add_to_group("enemies")
	if data:
		health.max_health = data.max_health
		health.max_poise = data.max_poise
		move_speed = data.move_speed
		attack_hitbox.damage = data.contact_damage
		health.health = data.max_health
	_base_color = visual.color
	health.died.connect(_on_died)
	health.poise_broken.connect(_on_poise_broken)

func _physics_process(delta: float) -> void:
	_player = _find_player()
	match state:
		State.IDLE:
			velocity = Vector2.ZERO
			if _player and _dist_to_player() <= aggro_range:
				state = State.CHASE
		State.CHASE:
			_do_chase()
		State.TELEGRAPH, State.ATTACK, State.RECOVER, State.STAGGERED:
			velocity = Vector2.ZERO
			_tick(delta)
	move_and_slide()

func _do_chase() -> void:
	if _player == null:
		state = State.IDLE
		return
	var d := _dist_to_player()
	if d > aggro_range * 1.3:
		state = State.IDLE
		return
	if d <= attack_range:
		_enter_telegraph()
		return
	var dir := (_player.global_position - global_position).normalized()
	velocity = (dir * move_speed) + _separation()

## Empurra para longe de Ecoados próximos — evita que fiquem sobrepostos.
func _separation() -> Vector2:
	var push := Vector2.ZERO
	for e in get_tree().get_nodes_in_group("enemies"):
		if e == self or not (e is Node2D):
			continue
		var offset := global_position - (e as Node2D).global_position
		var dist := offset.length()
		if dist > 0.0 and dist < separation_radius:
			push += offset.normalized() * (1.0 - dist / separation_radius)
	return push * separation_strength

func _tick(delta: float) -> void:
	_timer -= delta
	if _timer > 0.0:
		return
	match state:
		State.TELEGRAPH:
			_enter_attack()
		State.ATTACK:
			attack_hitbox.deactivate()
			state = State.RECOVER
			_timer = recover_time
		State.RECOVER:
			state = State.CHASE
		State.STAGGERED:
			health.recover_poise()
			state = State.CHASE

func _enter_telegraph() -> void:
	state = State.TELEGRAPH
	_timer = telegraph_time
	visual.color = Color(1, 1, 1)  ## aviso visual — o "tell" (§3.2)

func _enter_attack() -> void:
	state = State.ATTACK
	_timer = attack_time
	visual.color = _base_color
	if _player:
		var dir := (_player.global_position - global_position).normalized()
		attack_hitbox.position = dir * 14.0
	attack_hitbox.activate()

## Chamado pelo parry do jogador / Canção do Mundo (§3.1/§3.2) — abre finalização.
func stagger() -> void:
	state = State.STAGGERED
	_timer = 1.5
	attack_hitbox.deactivate()
	visual.color = _base_color

func _on_poise_broken() -> void:
	stagger()

func _dist_to_player() -> float:
	return global_position.distance_to(_player.global_position) if _player else INF

func _find_player() -> Node2D:
	var players := get_tree().get_nodes_in_group("player")
	return players[0] if not players.is_empty() else null

func _on_died() -> void:
	var reward: int = data.ecos_reward if data else 15
	SaveManager.state["aria"]["ecos"] += reward
	GameEvents.ecos_changed.emit(SaveManager.state["aria"]["ecos"])
	GameEvents.enemy_defeated.emit(data.id if data else &"dummy", global_position)
	queue_free()
