class_name BossEcoGuardia
extends CharacterBody2D
## Boss da Cripta das Guardiãs, em 2 fases.
## Fase 1: investida e varredura, ambas telegrafadas.
## Fase 2 (≤50% de vida): mais rápida e com investida tripla.
## Sem stagger (armadura de boss); reseta se a jogadora morrer ou descansar.

enum State { DORMANT, CHASE, TELEGRAPH, DASH, SWEEP, RECOVER, TRANSITION, DEAD }
enum AttackKind { DASH, SWEEP }

const ECHO_PICKUP := preload("res://world/echo_pickup.tscn")
# Tints de modulate sobre o sprite (que já é azulado).
const COLOR_PHASE1 := Color.WHITE
const COLOR_PHASE2 := Color(1.5, 0.75, 1.05)
const DASH_TIME := 0.3
const SWEEP_TIME := 0.25
const CHAIN_TELEGRAPH := 0.25

@export var boss_name := "Eco da Guardiã"
@export var bestiary_id := "eco_guardia"
@export var chase_speed := 60.0
@export var dash_speed := 330.0
@export var sweep_range := 48.0
@export var dash_range := 220.0
@export var echoes_reward := 500

var state := State.DORMANT
var phase := 1
var _timer := 0.0
var _attack := AttackKind.DASH
var _attack_dir := Vector2.RIGHT
var _dash_chain := 0
var _start_position := Vector2.ZERO

@onready var health: Health = $Health
@onready var hurtbox: Hurtbox = $Hurtbox
@onready var hitbox_pivot: Node2D = $HitboxPivot
@onready var dash_shape: CollisionShape2D = $HitboxPivot/DashHitbox/CollisionShape2D
@onready var sweep_shape: CollisionShape2D = $HitboxPivot/SweepHitbox/CollisionShape2D
@onready var visual: AnimatedSprite2D = $Visual


func _ready() -> void:
	_start_position = global_position
	hurtbox.hit_received.connect(_on_hit_received)
	health.changed.connect(_on_health_changed)
	health.died.connect(_on_died)
	var mult := GameState.enemy_health_mult()
	if mult != 1.0:
		health.max_health = int(round(health.max_health * mult))
		health.current = health.max_health


func _physics_process(delta: float) -> void:
	match state:
		State.DORMANT, State.DEAD:
			velocity = Vector2.ZERO
		State.CHASE:
			_state_chase()
		State.TELEGRAPH:
			_state_telegraph(delta)
		State.DASH:
			_state_dash(delta)
		State.SWEEP:
			_state_sweep(delta)
		State.RECOVER:
			velocity = Vector2.ZERO
			_timer -= delta
			if _timer <= 0.0:
				state = State.CHASE
		State.TRANSITION:
			velocity = Vector2.ZERO
			_timer -= delta
			visual.modulate = COLOR_PHASE1.lerp(COLOR_PHASE2, pingpong(_timer * 5.0, 1.0))
			if _timer <= 0.0:
				_finish_transition()
	move_and_slide()
	if absf(velocity.x) > 1.0:
		visual.flip_h = velocity.x < 0.0


func is_engaged() -> bool:
	return state != State.DORMANT and state != State.DEAD


func is_defeated() -> bool:
	return state == State.DEAD


func activate() -> void:
	if state != State.DORMANT:
		return
	state = State.CHASE
	GameEvents.boss_engaged.emit(boss_name, health.max_health)
	GameEvents.notify(boss_name.to_upper())
	FX.shake(9.0)


## Volta ao estado inicial (jogadora morreu ou descansou no santuário).
func reset() -> void:
	if not is_engaged():
		return
	state = State.DORMANT
	phase = 1
	_dash_chain = 0
	velocity = Vector2.ZERO
	global_position = _start_position
	health.heal_full()
	hurtbox.invulnerable = false
	_disable_hitboxes()
	visual.modulate = COLOR_PHASE1
	GameEvents.boss_ended.emit(false)


func _state_chase() -> void:
	var player := _get_player()
	if player == null:
		velocity = Vector2.ZERO
		return
	var to_player := player.global_position - global_position
	if to_player.length() <= sweep_range:
		_enter_telegraph(AttackKind.SWEEP, to_player.normalized())
	elif to_player.length() <= dash_range:
		_enter_telegraph(AttackKind.DASH, to_player.normalized())
	else:
		velocity = to_player.normalized() * chase_speed


func _enter_telegraph(kind: AttackKind, dir: Vector2) -> void:
	state = State.TELEGRAPH
	_attack = kind
	_attack_dir = dir
	hitbox_pivot.rotation = dir.angle()
	var base := 0.5 if kind == AttackKind.SWEEP else 0.55
	_timer = base if phase == 1 else base * 0.7
	velocity = Vector2.ZERO


func _state_telegraph(delta: float) -> void:
	_timer -= delta
	visual.modulate = _phase_color().lerp(Color(1, 0.95, 0.7), pingpong(_timer * 7.0, 1.0))
	if _timer > 0.0:
		return
	visual.modulate = _phase_color()
	if _attack == AttackKind.SWEEP:
		state = State.SWEEP
		_timer = SWEEP_TIME
		sweep_shape.set_deferred("disabled", false)
	else:
		state = State.DASH
		_timer = DASH_TIME
		dash_shape.set_deferred("disabled", false)


func _state_dash(delta: float) -> void:
	velocity = _attack_dir * dash_speed
	_timer -= delta
	if _timer > 0.0:
		return
	dash_shape.set_deferred("disabled", true)
	if phase == 2 and _dash_chain < 2:
		# Investida tripla: re-mira a cada elo da corrente.
		_dash_chain += 1
		var player := _get_player()
		if player:
			_attack_dir = (player.global_position - global_position).normalized()
			hitbox_pivot.rotation = _attack_dir.angle()
		state = State.TELEGRAPH
		_attack = AttackKind.DASH
		_timer = CHAIN_TELEGRAPH
	else:
		_dash_chain = 0
		_enter_recover()


func _state_sweep(delta: float) -> void:
	velocity = _attack_dir * 40.0
	hitbox_pivot.rotation = _attack_dir.angle() + lerpf(-1.4, 1.4, 1.0 - _timer / SWEEP_TIME)
	_timer -= delta
	if _timer <= 0.0:
		sweep_shape.set_deferred("disabled", true)
		_enter_recover()


func _enter_recover() -> void:
	state = State.RECOVER
	_timer = 1.0 if phase == 1 else 0.65


func _enter_transition() -> void:
	_disable_hitboxes()
	hurtbox.invulnerable = true
	state = State.TRANSITION
	_timer = 1.2
	velocity = Vector2.ZERO
	GameEvents.notify("A Guardiã lembra de quem era...")


func _finish_transition() -> void:
	phase = 2
	visual.modulate = COLOR_PHASE2
	hurtbox.invulnerable = false
	_enter_recover()


func _phase_color() -> Color:
	return COLOR_PHASE1 if phase == 1 else COLOR_PHASE2


func _get_player() -> Player:
	var p := get_tree().get_first_node_in_group("player") as Player
	if p and p.is_alive():
		return p
	return null


func _disable_hitboxes() -> void:
	dash_shape.set_deferred("disabled", true)
	sweep_shape.set_deferred("disabled", true)


## Aparado: interrompe o golpe e leva dano de postura (não atordoa por
## completo — bosses são resistentes), abrindo uma janela de punição.
func on_parried() -> void:
	if not is_engaged() or state == State.TRANSITION:
		return
	_disable_hitboxes()
	_dash_chain = 0
	_flash()
	AudioManager.play_sfx("parry")
	FX.hit_stop(0.09, 0.05)
	FX.shake(6.0)
	health.damage(28)
	if state == State.DEAD or state == State.TRANSITION:
		return
	_enter_recover()


func _on_hit_received(from_hitbox: Hitbox) -> void:
	if state == State.DORMANT or state == State.DEAD:
		return
	health.damage(from_hitbox.damage)
	_flash()
	AudioManager.play_sfx("hit")
	FX.hit_stop()
	FX.spawn_hit(global_position)


func _on_health_changed(current: int, max_value: int) -> void:
	if not is_engaged():
		return
	GameEvents.boss_health_changed.emit(current)
	if phase == 1 and state != State.TRANSITION and current > 0 \
			and current <= max_value / 2:
		_enter_transition()


func _flash() -> void:
	visual.modulate = Color(3.0, 3.0, 3.0)
	var tween := create_tween()
	tween.tween_property(visual, "modulate", _phase_color(), 0.12)


func _on_died() -> void:
	state = State.DEAD
	GameState.record_kill(bestiary_id)
	_disable_hitboxes()
	remove_from_group("enemies")
	collision_layer = 0
	set_collision_mask_value(2, false)
	set_collision_mask_value(3, false)
	visual.modulate = Color(0.35, 0.35, 0.45)
	var pickup := ECHO_PICKUP.instantiate()
	pickup.amount = int(round(echoes_reward * GameState.echo_mult()))
	pickup.position = global_position
	get_parent().add_child.call_deferred(pickup)
	GameEvents.boss_ended.emit(true)
	GameEvents.notify("O ECO FOI SILENCIADO")
