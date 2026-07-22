class_name FlyerEnemy
extends CharacterBody2D
## Ecoado Alado — inimigo voador (§3.2). Paira orbitando o jogador a uma distância,
## flutua (bob), e periodicamente MERGULHA num ataque telegrafado, recuando depois.
## Ignora paredes/abismos (collision_mask = 0). Combate justo: o mergulho avisa e
## viaja, dando tempo de rolar. Data-driven via EnemyData.

enum State { IDLE, HOVER, TELEGRAPH, DIVE, RETREAT, STAGGERED }

@export var data: EnemyData
@export var move_speed: float = 70.0
@export var aggro_range: float = 260.0
@export var preferred_range: float = 120.0
@export var telegraph_time: float = 0.5
@export var dive_time: float = 0.35
@export var retreat_time: float = 0.7
@export var dive_speed: float = 260.0

@onready var health: HealthComponent = $HealthComponent
@onready var visual: Polygon2D = $Visual
@onready var hurtbox: Hurtbox = $Hurtbox
@onready var dive_hitbox: Hitbox = $DiveHitbox

var state: State = State.IDLE
var _player: Node2D
var _timer: float = 0.0
var _base_color: Color
var _dormant: bool = false
var _orbit_dir: float = 1.0
var _bob_t: float = 0.0
var _dive_dir: Vector2 = Vector2.RIGHT

func _ready() -> void:
	add_to_group("enemies")
	if data:
		health.max_health = data.max_health
		health.max_poise = data.max_poise
		move_speed = data.move_speed
		dive_hitbox.damage = data.contact_damage
		health.health = data.max_health
		visual.color = data.tint
	var cyc := int(SaveManager.state.get("ng_cycle", 0))
	if cyc > 0:
		health.max_health *= NewGamePlus.health_mult(cyc)
		health.health = health.max_health
		dive_hitbox.damage *= NewGamePlus.damage_mult(cyc)
	_base_color = visual.color
	_orbit_dir = 1.0 if randf() < 0.5 else -1.0
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
		dive_hitbox.deactivate()

func _process(delta: float) -> void:
	# Flutuação vertical do corpo (só visual) — dá o "peso no ar".
	_bob_t += delta * 4.0
	visual.position.y = sin(_bob_t) * 3.0

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
				state = State.HOVER
				_timer = 1.0
		State.HOVER:
			_do_hover(delta)
		State.TELEGRAPH:
			velocity = Vector2.ZERO
			_tick(delta)
		State.DIVE:
			velocity = _dive_dir * dive_speed
			_tick(delta)
		State.RETREAT:
			if _player:
				velocity = (global_position - _player.global_position).normalized() * move_speed
			_tick(delta)
		State.STAGGERED:
			velocity = Vector2.ZERO
			_tick(delta)
	move_and_slide()

## Orbita o jogador mantendo distância; mergulha quando o tempo esgota.
func _do_hover(delta: float) -> void:
	if _player == null:
		state = State.IDLE
		return
	var d := _dist_to_player()
	if d > aggro_range * 1.3:
		state = State.IDLE
		return
	var dir := (_player.global_position - global_position).normalized()
	var tangent := Vector2(-dir.y, dir.x) * _orbit_dir
	var radial := 0.0
	if d > preferred_range * 1.1:
		radial = 1.0
	elif d < preferred_range * 0.9:
		radial = -1.0
	velocity = (dir * radial + tangent).normalized() * move_speed
	_timer -= delta
	if _timer <= 0.0 and d <= preferred_range * 1.6:
		_enter_telegraph()

func _enter_telegraph() -> void:
	state = State.TELEGRAPH
	_timer = telegraph_time
	visual.color = Color(1, 1, 1)  ## aviso do mergulho (§3.2)

func _tick(delta: float) -> void:
	_timer -= delta
	if _timer > 0.0:
		return
	match state:
		State.TELEGRAPH:
			# Trava a direção no jogador no instante do mergulho.
			if _player:
				_dive_dir = (_player.global_position - global_position).normalized()
			visual.color = _base_color
			dive_hitbox.activate()
			state = State.DIVE
			_timer = dive_time
		State.DIVE:
			dive_hitbox.deactivate()
			state = State.RETREAT
			_timer = retreat_time
		State.RETREAT:
			_orbit_dir *= -1.0  ## troca o sentido da órbita a cada ciclo
			state = State.HOVER
			_timer = 1.2
		State.STAGGERED:
			health.recover_poise()
			state = State.HOVER
			_timer = 1.0

func is_threatening() -> bool:
	return state == State.TELEGRAPH or state == State.DIVE

## Canção do Mundo / parry (§3.1/§3.2) — atordoa e derruba do voo.
func stagger() -> void:
	if state == State.STAGGERED:
		return
	state = State.STAGGERED
	_timer = 1.5
	dive_hitbox.deactivate()
	visual.color = _base_color

func _on_poise_broken() -> void:
	stagger()

func _dist_to_player() -> float:
	return global_position.distance_to(_player.global_position) if _player else INF

func _find_player() -> Node2D:
	var players := get_tree().get_nodes_in_group("player")
	return players[0] if not players.is_empty() else null

func _on_died() -> void:
	var reward: int = data.ecos_reward if data else 20
	SaveManager.state["aria"]["ecos"] += reward
	GameEvents.ecos_changed.emit(SaveManager.state["aria"]["ecos"])
	GameEvents.enemy_defeated.emit(data.id if data else &"alado", global_position)
	queue_free()
