class_name EnemyBase
extends CharacterBody2D
## Ecoado — IA base: persegue, telegrafa (pisca), ataca com investida,
## recupera (janela de punição) e dropa Ecos ao morrer.
## Variantes (soldado, brutamontes) só mudam os exports na cena.

enum State { IDLE, CHASE, TELEGRAPH, ATTACK, RECOVER, HURT, DEAD }

const ECHO_PICKUP := preload("res://world/echo_pickup.tscn")
const KNOCKBACK_DECAY := 500.0
const CALM_RANGE := 170.0
const CALM_DURATION := 4.0
const CALM_TINT := Color(0.6, 0.8, 1.25)

@export var max_speed := 50.0
@export var aggro_range := 140.0
@export var attack_range := 30.0
@export var telegraph_time := 0.5
@export var attack_time := 0.22
@export var attack_lunge := 180.0
@export var recover_time := 0.8
@export var hurt_time := 0.25
## 1.0 = sempre atordoa ao levar dano; inimigos pesados resistem (poise).
@export var stagger_chance := 1.0
@export var echoes_reward := 20

var state := State.IDLE
var _timer := 0.0
var _attack_dir := Vector2.RIGHT
var _knockback := Vector2.ZERO
var _calm_timer := 0.0

@onready var health: Health = $Health
@onready var hurtbox: Hurtbox = $Hurtbox
@onready var hitbox_pivot: Node2D = $HitboxPivot
@onready var hitbox_shape: CollisionShape2D = $HitboxPivot/Hitbox/CollisionShape2D
@onready var visual: AnimatedSprite2D = $Visual


func _ready() -> void:
	hurtbox.hit_received.connect(_on_hit_received)
	health.died.connect(_on_died)
	GameEvents.melody_played.connect(_on_melody_played)


func _physics_process(delta: float) -> void:
	_knockback = _knockback.move_toward(Vector2.ZERO, KNOCKBACK_DECAY * delta)
	if _calm_timer > 0.0:
		_calm_timer -= delta
		if _calm_timer <= 0.0:
			visual.modulate = Color.WHITE
	var player := _get_player()
	match state:
		State.IDLE:
			velocity = Vector2.ZERO
			# À noite os Ecoados ficam mais atentos (e mais generosos).
			var effective_range := aggro_range * (1.5 if GameState.is_night() else 1.0)
			if _calm_timer <= 0.0 and player \
					and global_position.distance_to(player.global_position) <= effective_range:
				state = State.CHASE
		State.CHASE:
			if player == null:
				velocity = Vector2.ZERO
				state = State.IDLE
			else:
				var to_player := player.global_position - global_position
				if to_player.length() <= attack_range:
					_enter_telegraph(to_player.normalized())
				else:
					velocity = to_player.normalized() * max_speed
		State.TELEGRAPH:
			velocity = Vector2.ZERO
			_timer -= delta
			visual.modulate = Color.WHITE.lerp(
					Color(2.2, 2.0, 1.2), pingpong(_timer * 6.0, 1.0))
			if _timer <= 0.0:
				_enter_attack()
		State.ATTACK:
			_timer -= delta
			velocity = _attack_dir * attack_lunge
			if _timer <= 0.0:
				_exit_attack()
		State.RECOVER:
			velocity = Vector2.ZERO
			_timer -= delta
			if _timer <= 0.0:
				state = State.CHASE
		State.HURT:
			velocity = Vector2.ZERO
			_timer -= delta
			if _timer <= 0.0:
				state = State.CHASE
		State.DEAD:
			velocity = Vector2.ZERO
	velocity += _knockback
	move_and_slide()
	if absf(velocity.x) > 1.0:
		visual.flip_h = velocity.x < 0.0


func _on_melody_played(melody_id: String) -> void:
	if melody_id != "acalento" or state == State.DEAD:
		return
	var player := _get_player()
	if player and global_position.distance_to(player.global_position) <= CALM_RANGE:
		calm(CALM_DURATION)


## O Acalento da ocarina: o Ecoado lembra por um instante do que era.
func calm(duration: float) -> void:
	_calm_timer = duration
	state = State.IDLE
	velocity = Vector2.ZERO
	hitbox_shape.set_deferred("disabled", true)
	visual.modulate = CALM_TINT


func _get_player() -> Player:
	var p := get_tree().get_first_node_in_group("player") as Player
	if p and p.is_alive():
		return p
	return null


func _enter_telegraph(dir: Vector2) -> void:
	state = State.TELEGRAPH
	_timer = telegraph_time
	_attack_dir = dir
	hitbox_pivot.rotation = dir.angle()


func _enter_attack() -> void:
	state = State.ATTACK
	_timer = attack_time
	visual.modulate = Color.WHITE
	hitbox_shape.set_deferred("disabled", false)


func _exit_attack() -> void:
	hitbox_shape.set_deferred("disabled", true)
	state = State.RECOVER
	_timer = recover_time


func _on_hit_received(from_hitbox: Hitbox) -> void:
	if state == State.DEAD:
		return
	health.damage(from_hitbox.damage)
	_flash()
	AudioManager.play_sfx("hit")
	if state == State.DEAD:
		return
	_knockback = (global_position - from_hitbox.global_position).normalized() \
			* from_hitbox.knockback
	if randf() <= stagger_chance:
		hitbox_shape.set_deferred("disabled", true)
		state = State.HURT
		_timer = hurt_time


func _flash() -> void:
	visual.modulate = Color(3.0, 3.0, 3.0)
	var tween := create_tween()
	tween.tween_property(visual, "modulate", Color.WHITE, 0.15)


func _on_died() -> void:
	state = State.DEAD
	hitbox_shape.set_deferred("disabled", true)
	AudioManager.play_sfx("enemy_death")
	var pickup := ECHO_PICKUP.instantiate()
	pickup.amount = int(echoes_reward * (1.5 if GameState.is_night() else 1.0))
	pickup.position = global_position
	get_parent().add_child.call_deferred(pickup)
	queue_free()
