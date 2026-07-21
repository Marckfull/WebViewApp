class_name SeleneBoss
extends CharacterBody2D
## Selene, a Guardiã Caída — boss final do Coração Mudo (§2, §3.2). Duas fases:
##
## Fase 1 — a Guardiã que ataca para não ser ouvida: padrões TELEGRAFADOS, como a
##   Guardiã do Eco, mas mais rápida.
## Fase 2 (50% de vida) — o DUELO DE MELODIAS: o aço não a alcança mais. Aria tem
##   de tocar a Canção do Mundo; cada vez que toca, Selene canta junto sem querer
##   e sua resistência cede. Ao completar a harmonia, o combate vira escolha (§2)
##   — o EndingScreen assume com os três finais.

enum State { CHASE, TELEGRAPH, ATTACK, RECOVER, PHASE2_INTRO, STAGGERED, DEAD }

@export var boss_name: String = "Selene, a Guardiã Caída"
@export var chase_speed: float = 62.0
@export var attack_range: float = 44.0
@export var telegraph_time: float = 0.55
@export var attack_time: float = 0.2
@export var recover_time: float = 0.7
@export var ecos_reward: int = 400

@onready var health: HealthComponent = $HealthComponent
@onready var sweep_hitbox: Hitbox = $SweepHitbox
@onready var visual: Polygon2D = $Visual

var state: State = State.CHASE
var phase: int = 1
var _timer: float = 0.0
var _player: Node2D
var _base_color: Color
var _duel: bool = false
var _harmony: int = 0

func _ready() -> void:
	var defeated: Array = SaveManager.state["world"].get("bosses_defeated", [])
	if defeated.has("selene"):
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
	GameEvents.melody_played.connect(_on_melody_played)
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
	visual.color = Color(1, 1, 1)

func _enter_attack() -> void:
	state = State.ATTACK
	_timer = attack_time
	visual.color = _base_color
	if _player:
		var dir := (_player.global_position - global_position).normalized()
		sweep_hitbox.position = dir * 26.0
	sweep_hitbox.activate()

func _on_health_changed(current: float, maximum: float) -> void:
	# No duelo o aço não a mata: mantém a vida no limiar e ignora a barra de vida
	# (a barra passa a refletir a harmonia da canção).
	if _duel:
		if current <= maximum * 0.5:
			health.health = maximum * 0.5 + 1.0
		return
	GameEvents.boss_health_changed.emit(current, maximum)
	if phase == 1 and current <= maximum * 0.5:
		_enter_phase2()

func _enter_phase2() -> void:
	phase = 2
	_duel = true
	_harmony = 0
	state = State.PHASE2_INTRO
	_timer = 1.2
	chase_speed *= 1.3
	telegraph_time = maxf(telegraph_time - 0.15, 0.3)
	visual.color = Color(0.5, 0.7, 1.0)
	sweep_hitbox.deactivate()
	GameEvents.boss_phase_changed.emit(2)
	# A barra começa cheia: cada Canção do Mundo a esvazia até a harmonia.
	GameEvents.boss_health_changed.emit(health.max_health, health.max_health)

## Cada Canção do Mundo cede a resistência de Selene (§2). Ao completar a
## harmonia, o duelo resolve e o EndingScreen assume com a escolha do fim.
func _on_melody_played(melody_id: StringName) -> void:
	if not _duel or state == State.DEAD:
		return
	if melody_id != &"cancao_do_mundo":
		return
	_harmony += 1
	var remaining := float(Endings.DUEL_HARMONY_NEEDED - _harmony)
	var frac := clampf(remaining / float(Endings.DUEL_HARMONY_NEEDED), 0.0, 1.0)
	GameEvents.boss_health_changed.emit(frac * health.max_health, health.max_health)
	visual.modulate = Color(2, 2, 3)
	create_tween().tween_property(visual, "modulate", Color(1, 1, 1), 0.3)
	if _harmony >= Endings.DUEL_HARMONY_NEEDED:
		_resolve_duel()

func _resolve_duel() -> void:
	state = State.DEAD
	sweep_hitbox.deactivate()
	SaveManager.state["aria"]["ecos"] += ecos_reward
	GameEvents.ecos_changed.emit(SaveManager.state["aria"]["ecos"])
	GameEvents.boss_defeated.emit(&"selene")
	GameEvents.enemy_defeated.emit(&"selene", global_position)
	queue_free()

## A Canção do Mundo também a acalma (ela canta junto sem querer) — reusa o efeito
## CALM_ENEMIES da ocarina, dando a janela de leitura do duelo.
func stagger() -> void:
	if state == State.DEAD:
		return
	state = State.STAGGERED
	_timer = 1.2
	sweep_hitbox.deactivate()

func is_threatening() -> bool:
	return state == State.TELEGRAPH or state == State.ATTACK

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
	# Só ocorre na fase 1 se algo a levar a 0 antes do duelo; no duelo a vida é
	# travada e a resolução vem por _resolve_duel().
	if _duel:
		return
	_resolve_duel()
