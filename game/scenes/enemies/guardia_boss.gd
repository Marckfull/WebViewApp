class_name GuardiaBoss
extends CharacterBody2D
## Guardiã do Eco — boss da Cripta (§3.2). Filosofia: padrões legíveis, ataques
## TELEGRAFADOS (nunca injusto), 2 fases. Fase 2 a 50% de vida: mais rápida,
## telegrafo mais curto e um avanço (lunge). Exige leitura de timing (Souls) —
## o par com a filosofia Zelda entra quando a dungeon ganhar sua mecânica-chave.

enum State { CHASE, TELEGRAPH, ATTACK, RECOVER, PHASE2_INTRO, STAGGERED, DEAD }

@export var boss_name: String = "Guardiã do Eco"
@export var chase_speed: float = 55.0
@export var attack_range: float = 42.0
@export var telegraph_time: float = 0.6
@export var attack_time: float = 0.2
@export var recover_time: float = 0.8
@export var ecos_reward: int = 200

@onready var health: HealthComponent = $HealthComponent
@onready var sweep_hitbox: Hitbox = $SweepHitbox
@onready var visual: Polygon2D = $Visual

var state: State = State.CHASE
var phase: int = 1
var _timer: float = 0.0
var _player: Node2D
var _base_color: Color

func _ready() -> void:
	# Boss já derrotado numa sessão anterior não reaparece (§3.2, persistência).
	var defeated: Array = SaveManager.state["world"].get("bosses_defeated", [])
	if defeated.has("guardia_do_eco"):
		queue_free()
		return
	add_to_group("enemies")
	add_to_group("boss")
	_base_color = visual.color
	health.health_changed.connect(_on_health_changed)
	health.died.connect(_on_died)
	health.poise_broken.connect(_on_poise_broken)
	($Hurtbox as Hurtbox).hit_taken.connect(_on_hurt)
	# Adiado: o HUD (último filho da cena) precisa estar conectado antes do anúncio.
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
		State.TELEGRAPH, State.ATTACK, State.RECOVER, State.PHASE2_INTRO, State.STAGGERED:
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
	visual.color = Color(1, 1, 1)  ## clarão de aviso — o "tell" (§3.2)

func _enter_attack() -> void:
	state = State.ATTACK
	_timer = attack_time
	visual.color = _base_color
	# Posiciona o golpe na direção do jogador e ativa a janela de acerto.
	if _player:
		var dir := (_player.global_position - global_position).normalized()
		sweep_hitbox.position = dir * 26.0
	sweep_hitbox.activate()

func _on_health_changed(current: float, maximum: float) -> void:
	GameEvents.boss_health_changed.emit(current, maximum)
	if phase == 1 and current <= maximum * 0.5:
		_enter_phase2()

func _enter_phase2() -> void:
	phase = 2
	state = State.PHASE2_INTRO
	_timer = 1.0
	chase_speed *= 1.4
	telegraph_time = maxf(telegraph_time - 0.2, 0.25)
	visual.color = Color(1, 0.4, 0.4)
	sweep_hitbox.deactivate()
	GameEvents.boss_phase_changed.emit(2)

## Flash + faísca de dano (§5) — modulate independente da cor de telegrafo/fase.
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
	GameEvents.boss_defeated.emit(&"guardia_do_eco")
	GameEvents.enemy_defeated.emit(&"guardia_do_eco", global_position)
	queue_free()
