class_name Player
extends CharacterBody2D
## Aria — protagonista. Máquina de estados: mover, rolar (i-frames),
## atacar (custo de stamina), dano e morte com drop de Ecos.

signal died

enum State { MOVE, ROLL, ATTACK, HURT, DEAD, GRAPPLE, PARRY }

const SPEED := 90.0
const ROLL_SPEED := 200.0
const ROLL_DURATION := 0.32
const ROLL_COST := 22.0
const ATTACK_COST := 14.0
const ATTACK_DURATION := 0.3
const ATTACK_HIT_START := 0.06
const ATTACK_HIT_END := 0.2
const ATTACK_LUNGE := 55.0
## Combo de 3 golpes: o 3º é uma finalização mais larga, forte e longe.
const FINISHER_LUNGE := 95.0
const FINISHER_MULT := 1.7
const BASE_KNOCKBACK := 150.0
const FINISHER_KNOCKBACK := 250.0
const HURT_DURATION := 0.35
const HURT_IFRAMES := 0.7
const KNOCKBACK_DECAY := 480.0
const LOCK_RANGE := 180.0
const LOCK_BREAK_RANGE := 240.0
const GRAPPLE_RANGE := 170.0
const GRAPPLE_SPEED := 400.0
const BASE_DAMAGE := 12
const DAMAGE_PER_FORGE := 4
const FLASK_HEAL := 60
const BASE_HEALTH := 100
const BASE_STAMINA := 100.0
const BOMB_SCENE := preload("res://world/bomb.tscn")
const BOMB_COOLDOWN := 2.0
const PARRY_COST := 12.0
const PARRY_DURATION := 0.4
const PARRY_IFRAMES := 0.35

var state: State = State.MOVE
var facing := Vector2.DOWN
var lock_target: Node2D
var _timer := 0.0
var _roll_dir := Vector2.ZERO
var _knockback := Vector2.ZERO
var _iframes := 0.0
var _grapple_target := Vector2.ZERO
var _bomb_cooldown := 0.0
var _parry_active := 0.0
var _push_accum := Vector2.ZERO
var _combo := 0
var _combo_queued := false
var _base_hit_damage := 12
var _sweep_from := -0.9
var _sweep_to := 0.9
var _lunge := ATTACK_LUNGE

@onready var health: Health = $Health
@onready var stamina: Stamina = $Stamina
@onready var hurtbox: Hurtbox = $Hurtbox
@onready var hitbox_pivot: Node2D = $HitboxPivot
@onready var hitbox: Hitbox = $HitboxPivot/Hitbox
@onready var hitbox_shape: CollisionShape2D = $HitboxPivot/Hitbox/CollisionShape2D
@onready var sword_visual: Polygon2D = $HitboxPivot/SwordVisual
@onready var sprite: AnimatedSprite2D = $Sprite
@onready var reticle: Polygon2D = $Reticle


func _ready() -> void:
	hurtbox.hit_received.connect(_on_hit_received)
	health.died.connect(_on_died)
	GameState.weapon_changed.connect(_apply_weapon_level)
	GameState.attributes_changed.connect(_apply_stats)
	GameState.inventory_changed.connect(_apply_stats)
	_apply_stats()
	health.heal_full()
	stamina.refill()


## Recalcula vida/vigor/dano a partir de atributos, amuletos e forja.
func _apply_stats() -> void:
	var max_hp := BASE_HEALTH + 10 * int(GameState.attributes["vit"])
	if GameState.has_item("amuleto_eco"):
		max_hp += 20
	health.max_health = max_hp
	health.current = mini(health.current, max_hp)
	health.changed.emit(health.current, max_hp)
	var max_stamina := BASE_STAMINA + 8.0 * int(GameState.attributes["fol"])
	if GameState.has_item("amuleto_vento"):
		max_stamina += 20.0
	stamina.max_stamina = max_stamina
	stamina.current = minf(stamina.current, max_stamina)
	stamina.changed.emit(stamina.current, max_stamina)
	_apply_weapon_level(GameState.weapon_level)


func _apply_weapon_level(level: int) -> void:
	_base_hit_damage = BASE_DAMAGE + DAMAGE_PER_FORGE * level \
			+ 2 * int(GameState.attributes["forca"])
	hitbox.damage = _base_hit_damage


## O Talismã de Sela reduz o custo de vigor em 20%.
func _stamina_cost(base: float) -> float:
	return base * (0.8 if GameState.has_item("talisma_sela") else 1.0)


func _physics_process(delta: float) -> void:
	_iframes = maxf(_iframes - delta, 0.0)
	_bomb_cooldown = maxf(_bomb_cooldown - delta, 0.0)
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
		State.GRAPPLE:
			_state_grapple(delta)
		State.PARRY:
			_state_parry(delta)
		State.DEAD:
			velocity = Vector2.ZERO
	velocity += _knockback + _push_accum
	move_and_slide()
	_push_accum = Vector2.ZERO
	_update_animation()


## Força externa contínua (correntes de vento). Somada uma vez por quadro
## e zerada em seguida — sem acúmulo entre zonas.
func push(force: Vector2) -> void:
	_push_accum += force


## Dano de ambiente (correntes de sal, etc.): sem knockback nem troca de
## estado, mas respeita i-frames e pode matar.
func take_environmental_damage(amount: int) -> void:
	if state == State.DEAD or _iframes > 0.0:
		return
	health.damage(int(round(amount * GameState.enemy_damage_mult())))
	if state == State.DEAD:
		return
	_flash()
	AudioManager.play_sfx("hurt")


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
			and stamina.try_spend(_stamina_cost(ROLL_COST)):
		_enter_roll(dir.normalized())
	elif Input.is_action_just_pressed("attack") \
			and stamina.try_spend(_stamina_cost(ATTACK_COST)):
		_enter_attack()
	elif Input.is_action_just_pressed("use_item"):
		_try_grapple()
	elif Input.is_action_just_pressed("heal"):
		_drink_flask()
	elif Input.is_action_just_pressed("bomb"):
		_throw_bomb()
	elif Input.is_action_just_pressed("parry") \
			and stamina.try_spend(_stamina_cost(PARRY_COST)):
		_enter_parry()


func _enter_roll(dir: Vector2) -> void:
	state = State.ROLL
	_timer = ROLL_DURATION
	_roll_dir = dir
	hurtbox.invulnerable = true
	sprite.modulate = Color(1.0, 1.0, 1.0, 0.45)
	AudioManager.play_sfx("roll")


func _state_roll(delta: float) -> void:
	velocity = _roll_dir * ROLL_SPEED
	_timer -= delta
	if _timer <= 0.0:
		hurtbox.invulnerable = false
		sprite.modulate = Color.WHITE
		state = State.MOVE


func _enter_attack(combo := 0) -> void:
	state = State.ATTACK
	_timer = ATTACK_DURATION
	_combo = combo
	_combo_queued = false
	sword_visual.visible = true
	# play() direto (fora do guard de _update_animation) para reiniciar
	# a animação não-loop a cada golpe.
	sprite.flip_h = _is_side() and facing.x < 0.0
	sprite.play("attack_" + _facing_name())
	# Cada elo do combo alterna a varredura; o 3º é a finalização.
	match combo:
		0:
			_sweep_from = -0.9
			_sweep_to = 0.9
			_lunge = ATTACK_LUNGE
			hitbox.damage = _base_hit_damage
			hitbox.knockback = BASE_KNOCKBACK
		1:
			_sweep_from = 0.9
			_sweep_to = -0.9
			_lunge = ATTACK_LUNGE
			hitbox.damage = _base_hit_damage
			hitbox.knockback = BASE_KNOCKBACK
		_:
			_sweep_from = -1.3
			_sweep_to = 1.3
			_lunge = FINISHER_LUNGE
			hitbox.damage = int(round(_base_hit_damage * FINISHER_MULT))
			hitbox.knockback = FINISHER_KNOCKBACK
	AudioManager.play_sfx("crit" if combo >= 2 else "swing")


func _state_attack(delta: float) -> void:
	_timer -= delta
	var t := ATTACK_DURATION - _timer
	# Varredura do golpe: o pivô gira ao redor da direção encarada.
	hitbox_pivot.rotation = facing.angle() + lerpf(_sweep_from, _sweep_to, t / ATTACK_DURATION)
	velocity = facing * _lunge * maxf(1.0 - t / ATTACK_DURATION, 0.0)
	var active := t >= ATTACK_HIT_START and t <= ATTACK_HIT_END
	hitbox_shape.set_deferred("disabled", not active)
	# Encadear: apertar ataque na 2ª metade do golpe compra o próximo elo.
	if _combo < 2 and not _combo_queued and t >= ATTACK_HIT_START \
			and Input.is_action_just_pressed("attack") \
			and stamina.try_spend(_stamina_cost(ATTACK_COST)):
		_combo_queued = true
	if _timer <= 0.0:
		if _combo_queued and _combo < 2:
			_enter_attack(_combo + 1)
		else:
			_exit_attack()


func _exit_attack() -> void:
	sword_visual.visible = false
	hitbox_shape.set_deferred("disabled", true)
	_combo = 0
	_combo_queued = false
	state = State.MOVE


func _state_hurt(delta: float) -> void:
	velocity = Vector2.ZERO
	_timer -= delta
	if _timer <= 0.0:
		state = State.MOVE


func _enter_parry() -> void:
	state = State.PARRY
	_timer = PARRY_DURATION
	_parry_active = GameState.parry_window()
	velocity = Vector2.ZERO
	sprite.modulate = Color(0.7, 0.9, 1.4)
	AudioManager.play_sfx("blip")


func _state_parry(delta: float) -> void:
	velocity = Vector2.ZERO
	_parry_active = maxf(_parry_active - delta, 0.0)
	_timer -= delta
	if _timer <= 0.0:
		sprite.modulate = Color.WHITE
		state = State.MOVE


## Aparou no tempo certo: atordoa o inimigo (abre para o crítico).
func _resolve_parry(from_hitbox: Hitbox) -> void:
	var src := from_hitbox.source()
	if src and src.has_method("on_parried"):
		src.on_parried()
	_iframes = PARRY_IFRAMES
	AudioManager.play_sfx("parry")
	FX.hit_stop(0.09, 0.05)
	FX.shake(5.0)
	FX.spawn_hit(from_hitbox.global_position, Color(1.0, 0.95, 0.6))
	sprite.modulate = Color(1.6, 1.6, 2.0)
	var tween := create_tween()
	tween.tween_property(sprite, "modulate", Color.WHITE, 0.25)


func _on_hit_received(from_hitbox: Hitbox) -> void:
	# Parry tem prioridade: no tempo certo, converte o golpe em atordoamento.
	if state == State.PARRY and _parry_active > 0.0 and from_hitbox.parryable:
		_resolve_parry(from_hitbox)
		return
	if _iframes > 0.0 or state == State.DEAD or state == State.ROLL:
		return
	health.damage(int(round(from_hitbox.damage * GameState.enemy_damage_mult())))
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
	AudioManager.play_sfx("hurt")
	FX.shake(6.0)
	FX.spawn_hit(global_position, Color(1.0, 0.5, 0.5))


func _flash() -> void:
	sprite.modulate = Color(3.0, 1.2, 1.2)
	var tween := create_tween()
	tween.tween_property(sprite, "modulate", Color.WHITE, 0.2)


func _on_died() -> void:
	if state == State.ATTACK:
		_exit_attack()
	lock_target = null
	state = State.DEAD
	sprite.modulate = Color(0.45, 0.45, 0.55, 0.6)
	died.emit()
	GameEvents.player_died.emit(global_position)


func respawn(at: Vector2) -> void:
	global_position = at
	$Camera2D.reset_smoothing()
	health.heal_full()
	stamina.refill()
	GameState.refill_flasks()
	_knockback = Vector2.ZERO
	_iframes = 1.0
	sprite.modulate = Color.WHITE
	state = State.MOVE


func _drink_flask() -> void:
	if health.current >= health.max_health:
		return
	if not GameState.use_flask():
		GameEvents.notify("Sem Essência. Descanse num santuário.")
		return
	health.heal(FLASK_HEAL)
	AudioManager.play_sfx("drink")
	sprite.modulate = Color(0.6, 1.6, 0.7)
	var tween := create_tween()
	tween.tween_property(sprite, "modulate", Color.WHITE, 0.35)


func _throw_bomb() -> void:
	if not GameState.has_item("bomba_eco") or _bomb_cooldown > 0.0:
		return
	_bomb_cooldown = BOMB_COOLDOWN
	var bomb := BOMB_SCENE.instantiate()
	bomb.position = global_position + facing * 28.0
	get_parent().add_child(bomb)
	AudioManager.play_sfx("blip")


func _try_grapple() -> void:
	if not GameState.has_item("gancho_corda"):
		return
	var best: Node2D = null
	var best_dist := GRAPPLE_RANGE
	for point in get_tree().get_nodes_in_group("grapple_points"):
		var dist := global_position.distance_to(point.global_position)
		if dist < best_dist and dist > 20.0:
			best_dist = dist
			best = point
	if best == null:
		GameEvents.notify("Nenhum poste de gancho ao alcance.")
		return
	_grapple_target = best.global_position
	facing = (_grapple_target - global_position).normalized()
	state = State.GRAPPLE
	_timer = 1.0  # trava de segurança se algo bloquear o caminho
	AudioManager.play_sfx("roll")


func _state_grapple(delta: float) -> void:
	var to_target := _grapple_target - global_position
	_timer -= delta
	if to_target.length() < 12.0 or _timer <= 0.0:
		velocity = Vector2.ZERO
		state = State.MOVE
		return
	velocity = to_target.normalized() * GRAPPLE_SPEED


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


func _update_animation() -> void:
	sprite.flip_h = _is_side() and facing.x < 0.0
	match state:
		State.MOVE:
			var prefix := "walk_" if velocity.length() > 5.0 else "idle_"
			_play(prefix + _facing_name())
		State.ROLL, State.GRAPPLE:
			_play("roll_" + _facing_name())
		State.ATTACK:
			pass  # disparada uma única vez em _enter_attack (não-loop)
		State.HURT, State.PARRY:
			_play("idle_" + _facing_name())
		State.DEAD:
			_play("idle_down")


func _is_side() -> bool:
	return absf(facing.x) >= absf(facing.y)


func _facing_name() -> String:
	if _is_side():
		return "side"
	return "down" if facing.y > 0.0 else "up"


func _play(anim: String) -> void:
	if sprite.animation != StringName(anim):
		sprite.play(anim)


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
