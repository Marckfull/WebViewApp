class_name DungeonBoss
extends CharacterBody2D
## DungeonBoss — boss reutilizável das dungeons do Ato 2 (§3.2). Mesma filosofia
## da Guardiã do Eco (padrões legíveis, ataques TELEGRAFADOS, 2 fases), mas
## data-driven por @export: cada dungeon instancia com nome/id/vida/ritmo/cor
## próprios. A fase 2 (a 50% de vida) acelera e encurta o telegrafo.
##
## Persiste como derrotado (bosses_defeated) e escala em New Game+ (§3.6).

enum State { CHASE, TELEGRAPH, ATTACK, RECOVER, PHASE2_INTRO, STAGGERED, DEAD }

@export var boss_name: String = "Guardião"
@export var boss_id: StringName = &"boss"
@export var chase_speed: float = 58.0
@export var attack_range: float = 44.0
@export var telegraph_time: float = 0.6
@export var attack_time: float = 0.2
@export var recover_time: float = 0.8
@export var ecos_reward: int = 250
@export var phase2_speed_mult: float = 1.4
@export var phase2_color: Color = Color(1, 0.4, 0.4)

## Movesets-assinatura opcionais (§3.2) — cada dungeon liga os que fazem sentido:
@export var lunge_on_attack: bool = false        ## avança durante o golpe (Martelo Mudo)
@export var lunge_speed: float = 150.0
@export var projectiles_per_attack: int = 0      ## dispara projéteis no golpe (Sino/Maré)
@export var projectile_spread_deg: float = 30.0
@export var projectile_damage: float = 12.0
@export var projectile_poise: float = 6.0
@export var summon_scene: PackedScene            ## invoca capangas na fase 2 (Coro Enraizado)
@export var summon_count: int = 0

const PROJECTILE_SCENE := preload("res://scenes/combat/projectile.tscn")
const ENEMY_HITBOX_LAYER := 16
const PLAYER_HURTBOX_LAYER := 2

@onready var health: HealthComponent = $HealthComponent
@onready var sweep_hitbox: Hitbox = $SweepHitbox
@onready var visual: Polygon2D = $Visual

var state: State = State.CHASE
var phase: int = 1
var _timer: float = 0.0
var _player: Node2D
var _base_color: Color
var _lunge_dir: Vector2 = Vector2.ZERO

func _ready() -> void:
	var defeated: Array = SaveManager.state["world"].get("bosses_defeated", [])
	if defeated.has(String(boss_id)):
		queue_free()
		return
	add_to_group("enemies")
	add_to_group("boss")
	_base_color = visual.color
	var cyc := int(SaveManager.state.get("ng_cycle", 0))
	if cyc > 0:
		health.max_health *= NewGamePlus.health_mult(cyc)
		health.health = health.max_health
		sweep_hitbox.damage *= NewGamePlus.damage_mult(cyc)
	health.health_changed.connect(_on_health_changed)
	health.died.connect(_on_died)
	health.poise_broken.connect(_on_poise_broken)
	($Hurtbox as Hurtbox).hit_taken.connect(_on_hurt)
	_announce.call_deferred()

func _announce() -> void:
	GameEvents.boss_spawned.emit(boss_name, health.max_health)

func _physics_process(delta: float) -> void:
	if state == State.DEAD:
		return
	_player = get_tree().get_first_node_in_group("player") as Node2D
	match state:
		State.CHASE:
			_do_chase()
		State.ATTACK:
			# Alguns chefes avançam durante o golpe (Martelo Mudo, §3.2).
			velocity = _lunge_dir * lunge_speed if lunge_on_attack else Vector2.ZERO
			_tick(delta)
		State.TELEGRAPH, State.RECOVER, State.PHASE2_INTRO, State.STAGGERED:
			velocity = Vector2.ZERO
			_tick(delta)
	move_and_slide()

func _do_chase() -> void:
	if _player == null:
		velocity = Vector2.ZERO
		return
	var to_player := _player.global_position - global_position
	if to_player.length() <= attack_range:
		_enter_telegraph()
	else:
		velocity = to_player.normalized() * chase_speed

func _tick(delta: float) -> void:
	_timer -= delta
	if _timer > 0.0:
		return
	match state:
		State.TELEGRAPH:
			_enter_attack()
		State.ATTACK:
			sweep_hitbox.deactivate()
			state = State.RECOVER
			_timer = recover_time
		State.RECOVER:
			state = State.CHASE
		State.PHASE2_INTRO:
			visual.color = _base_color
			health.recover_poise()
			state = State.CHASE
		State.STAGGERED:
			health.recover_poise()
			state = State.CHASE

func _enter_telegraph() -> void:
	state = State.TELEGRAPH
	_timer = telegraph_time
	visual.color = Color(1, 1, 1)

func _enter_attack() -> void:
	state = State.ATTACK
	_timer = attack_time
	visual.color = _base_color
	if _player:
		var dir := (_player.global_position - global_position).normalized()
		sweep_hitbox.position = dir * 26.0
		_lunge_dir = dir
		if projectiles_per_attack > 0:
			_fire_volley(dir)
	sweep_hitbox.activate()

## Dispara um leque de projéteis na direção do jogador (Sino Invertido, Maré Salgada).
func _fire_volley(dir: Vector2) -> void:
	var spread := deg_to_rad(projectile_spread_deg)
	var base := dir.angle()
	for i in projectiles_per_attack:
		var offset := 0.0
		if projectiles_per_attack > 1:
			offset = spread * (float(i) / float(projectiles_per_attack - 1) - 0.5)
		var proj := PROJECTILE_SCENE.instantiate()
		get_tree().current_scene.add_child(proj)
		(proj as Node2D).global_position = global_position + Vector2.RIGHT.rotated(base + offset) * 20.0
		proj.setup(Vector2.RIGHT.rotated(base + offset), projectile_damage, projectile_poise, ENEMY_HITBOX_LAYER, PLAYER_HURTBOX_LAYER)

func _on_health_changed(current: float, maximum: float) -> void:
	GameEvents.boss_health_changed.emit(current, maximum)
	if phase == 1 and current <= maximum * 0.5:
		_enter_phase2()

func _enter_phase2() -> void:
	phase = 2
	state = State.PHASE2_INTRO
	_timer = 1.0
	chase_speed *= phase2_speed_mult
	telegraph_time = maxf(telegraph_time - 0.2, 0.25)
	visual.color = phase2_color
	sweep_hitbox.deactivate()
	if summon_scene != null and summon_count > 0:
		_summon()
	GameEvents.boss_phase_changed.emit(2)

## Invoca capangas ao redor de si na virada de fase (Coro Enraizado, §3.2).
func _summon() -> void:
	var host := get_tree().current_scene
	if host == null:
		return
	for i in summon_count:
		var minion := summon_scene.instantiate()
		host.add_child(minion)
		var angle := TAU * float(i) / float(maxi(summon_count, 1))
		(minion as Node2D).global_position = global_position + Vector2(cos(angle), sin(angle)) * 40.0

func is_threatening() -> bool:
	return state == State.TELEGRAPH or state == State.ATTACK

## A Canção do Mundo o acalma (§3.1) — reusa CALM_ENEMIES da ocarina.
func stagger() -> void:
	if state == State.DEAD:
		return
	state = State.STAGGERED
	_timer = 1.2
	sweep_hitbox.deactivate()

func _on_hurt(_hitbox: Hitbox) -> void:
	visual.modulate = Color(4, 4, 4)
	create_tween().tween_property(visual, "modulate", Color(1, 1, 1), 0.12)
	CombatFx.spark(self)

func _on_poise_broken() -> void:
	if state == State.DEAD:
		return
	state = State.STAGGERED
	_timer = 1.5
	sweep_hitbox.deactivate()

func _on_died() -> void:
	state = State.DEAD
	sweep_hitbox.deactivate()
	SaveManager.state["aria"]["ecos"] += ecos_reward
	GameEvents.ecos_changed.emit(SaveManager.state["aria"]["ecos"])
	GameEvents.boss_defeated.emit(boss_id)
	GameEvents.enemy_defeated.emit(boss_id, global_position)
	queue_free()
