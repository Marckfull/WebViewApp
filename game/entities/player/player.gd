class_name Player
extends CharacterBody2D
## Aria — protagonista. Máquina de estados: mover, rolar (i-frames),
## atacar (custo de stamina), dano e morte com drop de Ecos.

signal died

enum State { MOVE, ROLL, ATTACK, HURT, DEAD }

const SPEED := 90.0
const ROLL_SPEED := 200.0
const ROLL_DURATION := 0.32
const ROLL_COST := 22.0
const ATTACK_COST := 14.0
const ATTACK_DURATION := 0.3
const ATTACK_HIT_START := 0.06
const ATTACK_HIT_END := 0.2
const ATTACK_LUNGE := 55.0
const HURT_DURATION := 0.35
const HURT_IFRAMES := 0.7
const KNOCKBACK_DECAY := 480.0
const LOCK_RANGE := 180.0
const LOCK_BREAK_RANGE := 240.0

var state: State = State.MOVE
var facing := Vector2.DOWN
var lock_target: Node2D
var _timer := 0.0
var _roll_dir := Vector2.ZERO
var _knockback := Vector2.ZERO
var _iframes := 0.0

@onready var health: Health = $Health
@onready var stamina: Stamina = $Stamina
@onready var hurtbox: Hurtbox = $Hurtbox
@onready var hitbox_pivot: Node2D = $HitboxPivot
@onready var hitbox_shape: CollisionShape2D = $HitboxPivot/Hitbox/CollisionShape2D
@onready var sword_visual: Polygon2D = $HitboxPivot/SwordVisual
@onready var body_visual: Polygon2D = $BodyVisual
@onready var reticle: Polygon2D = $Reticle


func _ready() -> void:
	hurtbox.hit_received.connect(_on_hit_received)
	health.died.connect(_on_died)


func _physics_process(delta: float) -> void:
	_iframes = maxf(_iframes - delta, 0.0)
	_knockback = _knockback.move_toward(Vector2.ZERO, KNOCKBACK_DECAY * delta)
	_update_lock()
	match state:
		State.MOVE:
			_state_move()
		State.ROLL:
			_state_roll(delta)
		State.ATTACK:
			_state_attack(delta)
		State.HURT:
			_state_hurt(delta)
		State.DEAD:
			velocity = Vector2.ZERO
	velocity += _knockback
	move_and_slide()


func is_alive() -> bool:
	return state != State.DEAD


func _state_move() -> void:
	var dir := Input.get_vector("move_left", "move_right", "move_up", "move_down")
	velocity = dir * SPEED
	# Com lock-on ativo, o facing aponta para o alvo (strafe); sem lock,
	# segue a direção do movimento.
	if lock_target == null and dir.length() > 0.1:
		facing = dir.normalized()
	if Input.is_action_just_pressed("roll") and dir.length() > 0.1 \
			and stamina.try_spend(ROLL_COST):
		_enter_roll(dir.normalized())
	elif Input.is_action_just_pressed("attack") and stamina.try_spend(ATTACK_COST):
		_enter_attack()


func _enter_roll(dir: Vector2) -> void:
	state = State.ROLL
	_timer = ROLL_DURATION
	_roll_dir = dir
	hurtbox.invulnerable = true
	body_visual.modulate = Color(1.0, 1.0, 1.0, 0.45)


func _state_roll(delta: float) -> void:
	velocity = _roll_dir * ROLL_SPEED
	_timer -= delta
	if _timer <= 0.0:
		hurtbox.invulnerable = false
		body_visual.modulate = Color.WHITE
		state = State.MOVE


func _enter_attack() -> void:
	state = State.ATTACK
	_timer = ATTACK_DURATION
	sword_visual.visible = true


func _state_attack(delta: float) -> void:
	_timer -= delta
	var t := ATTACK_DURATION - _timer
	# Varredura do golpe: o pivô gira ao redor da direção encarada.
	hitbox_pivot.rotation = facing.angle() + lerpf(-0.9, 0.9, t / ATTACK_DURATION)
	velocity = facing * ATTACK_LUNGE * maxf(1.0 - t / ATTACK_DURATION, 0.0)
	var active := t >= ATTACK_HIT_START and t <= ATTACK_HIT_END
	hitbox_shape.set_deferred("disabled", not active)
	if _timer <= 0.0:
		_exit_attack()


func _exit_attack() -> void:
	sword_visual.visible = false
	hitbox_shape.set_deferred("disabled", true)
	state = State.MOVE


func _state_hurt(delta: float) -> void:
	velocity = Vector2.ZERO
	_timer -= delta
	if _timer <= 0.0:
		state = State.MOVE


func _on_hit_received(from_hitbox: Hitbox) -> void:
	if _iframes > 0.0 or state == State.DEAD or state == State.ROLL:
		return
	health.damage(from_hitbox.damage)
	if state == State.DEAD:
		return
	_iframes = HURT_IFRAMES
	_knockback = (global_position - from_hitbox.global_position).normalized() \
			* from_hitbox.knockback
	if state == State.ATTACK:
		_exit_attack()
	state = State.HURT
	_timer = HURT_DURATION
	_flash()


func _flash() -> void:
	body_visual.modulate = Color(3.0, 1.2, 1.2)
	var tween := create_tween()
	tween.tween_property(body_visual, "modulate", Color.WHITE, 0.2)


func _on_died() -> void:
	if state == State.ATTACK:
		_exit_attack()
	lock_target = null
	state = State.DEAD
	body_visual.modulate = Color(0.45, 0.45, 0.55, 0.6)
	died.emit()
	GameEvents.player_died.emit(global_position)


func respawn(at: Vector2) -> void:
	global_position = at
	$Camera2D.reset_smoothing()
	health.heal_full()
	stamina.refill()
	_knockback = Vector2.ZERO
	_iframes = 1.0
	body_visual.modulate = Color.WHITE
	state = State.MOVE


func _update_lock() -> void:
	if Input.is_action_just_pressed("lock_on"):
		_cycle_lock_target()
	if lock_target and (not is_instance_valid(lock_target)
			or not lock_target.is_in_group("enemies")
			or global_position.distance_to(lock_target.global_position) > LOCK_BREAK_RANGE):
		lock_target = null
	if lock_target and state != State.ROLL and state != State.DEAD:
		facing = (lock_target.global_position - global_position).normalized()
	reticle.visible = lock_target != null
	if lock_target:
		reticle.global_position = lock_target.global_position + Vector2(0, -20)


func _cycle_lock_target() -> void:
	var candidates: Array = get_tree().get_nodes_in_group("enemies").filter(
			func(e: Node2D) -> bool:
				return global_position.distance_to(e.global_position) <= LOCK_RANGE)
	if candidates.is_empty():
		lock_target = null
		return
	candidates.sort_custom(
			func(a: Node2D, b: Node2D) -> bool:
				return global_position.distance_squared_to(a.global_position) \
						< global_position.distance_squared_to(b.global_position))
	var idx := candidates.find(lock_target)
	lock_target = candidates[(idx + 1) % candidates.size()] if idx >= 0 else candidates[0]
