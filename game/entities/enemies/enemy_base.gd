class_name EnemyBase
extends CharacterBody2D
## Ecoado — IA base: persegue, telegrafa (pisca), ataca com investida,
## recupera (janela de punição) e dropa Ecos ao morrer.
## Variantes (soldado, brutamontes) só mudam os exports na cena.

enum State { IDLE, CHASE, TELEGRAPH, ATTACK, RECOVER, HURT, DEAD, STUNNED }

const ECHO_PICKUP := preload("res://world/echo_pickup.tscn")
const KNOCKBACK_DECAY := 500.0
const CALM_RANGE := 170.0
const CALM_DURATION := 4.0
const CALM_TINT := Color(0.6, 0.8, 1.25)
const STUN_DURATION := 1.8
const STUN_TINT := Color(1.5, 1.4, 0.5)
## Multiplicador de dano ao atingir um inimigo atordoado (finalização).
const CRIT_MULT := 2.5

@export var max_speed := 50.0
@export var aggro_range := 140.0
@export var attack_range := 30.0
@export var telegraph_time := 0.5
@export var attack_time := 0.22
@export var attack_lunge := 180.0
## Desligue para inimigos à distância (que atacam via _on_attack_start).
@export var melee_enabled := true
@export var recover_time := 0.8
@export var hurt_time := 0.25
## 1.0 = sempre atordoa ao levar dano; inimigos pesados resistem (poise).
@export var stagger_chance := 1.0
## Golpes seguidos para quebrar a postura (0 = só o parry atordoa).
@export var poise_max := 0.0
@export var poise_regen := 2.5
@export var echoes_reward := 20

var state := State.IDLE
var _timer := 0.0
var _attack_dir := Vector2.RIGHT
var _knockback := Vector2.ZERO
var _calm_timer := 0.0
var _poise := 0.0

@onready var health: Health = $Health
@onready var hurtbox: Hurtbox = $Hurtbox
@onready var hitbox_pivot: Node2D = $HitboxPivot
@onready var hitbox_shape: CollisionShape2D = $HitboxPivot/Hitbox/CollisionShape2D
@onready var visual: AnimatedSprite2D = $Visual


func _ready() -> void:
	hurtbox.hit_received.connect(_on_hit_received)
	health.died.connect(_on_died)
	GameEvents.melody_played.connect(_on_melody_played)
	_scale_health()


## Vida escalada pelo ciclo de New Game+.
func _scale_health() -> void:
	var mult := GameState.enemy_health_mult()
	if mult != 1.0:
		health.max_health = int(round(health.max_health * mult))
		health.current = health.max_health
		health.changed.emit(health.current, health.max_health)


func _physics_process(delta: float) -> void:
	_knockback = _knockback.move_toward(Vector2.ZERO, KNOCKBACK_DECAY * delta)
	if _calm_timer > 0.0:
		_calm_timer -= delta
		if _calm_timer <= 0.0:
			visual.modulate = Color.WHITE
	if _poise > 0.0 and state != State.STUNNED:
		_poise = maxf(_poise - poise_regen * delta, 0.0)
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
		State.STUNNED:
			velocity = Vector2.ZERO
			_timer -= delta
			if _timer <= 0.0:
				visual.modulate = Color.WHITE
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


## Aparado pela jogadora: quebra de postura, aberto para a finalização.
func on_parried() -> void:
	if state == State.DEAD:
		return
	_enter_stun()


func _enter_stun() -> void:
	state = State.STUNNED
	_timer = STUN_DURATION
	_poise = 0.0
	velocity = Vector2.ZERO
	hitbox_shape.set_deferred("disabled", true)
	visual.modulate = STUN_TINT


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
	if melee_enabled:
		hitbox_shape.set_deferred("disabled", false)
	_on_attack_start()


## Gancho para subclasses: disparar projétil, invocar, etc. Chamado no
## início do golpe. _attack_dir já aponta para a jogadora.
func _on_attack_start() -> void:
	pass


func _exit_attack() -> void:
	hitbox_shape.set_deferred("disabled", true)
	state = State.RECOVER
	_timer = recover_time


func _on_hit_received(from_hitbox: Hitbox) -> void:
	if state == State.DEAD:
		return
	var stunned := state == State.STUNNED
	var dmg := from_hitbox.damage
	if stunned:
		dmg = int(round(dmg * CRIT_MULT))  # finalização
	health.damage(dmg)
	_flash()
	FX.hit_stop()
	if stunned:
		AudioManager.play_sfx("crit")
		FX.shake(6.0)
		FX.spawn_hit(global_position, Color(1.0, 0.85, 0.4))
	else:
		AudioManager.play_sfx("hit")
		FX.spawn_hit(global_position)
	if state == State.DEAD:
		return
	_knockback = (global_position - from_hitbox.global_position).normalized() \
			* from_hitbox.knockback
	if stunned:
		return  # segue atordoado; continua recebendo críticos
	# Postura: golpes seguidos quebram a guarda de inimigos pesados.
	if poise_max > 0.0:
		_poise += 1.0
		if _poise >= poise_max:
			_enter_stun()
			return
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
	var reward := echoes_reward * GameState.echo_mult()
	if GameState.is_night():
		reward *= 1.5
	pickup.amount = int(round(reward))
	pickup.position = global_position
	get_parent().add_child.call_deferred(pickup)
	queue_free()
