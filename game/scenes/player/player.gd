class_name Player
extends CharacterBody2D
## Aria — protagonista. Fase 0: mover (8-dir), atacar, esquivar (i-frames),
## defender/parry, lock-on. Combate governado por stamina (§3.2).
##
## Nós filhos esperados (montados na cena player.tscn):
##   HealthComponent, StaminaComponent, Hurtbox, LockOnSystem, AttackHitbox (Hitbox),
##   AnimationPlayer (opcional na Fase 0).

@export var speed: float = 90.0
@export var dodge_speed: float = 240.0
@export var dodge_duration: float = 0.28
@export var dodge_iframes: float = 0.18
@export var dodge_stamina: float = 25.0
@export var attack_stamina: float = 20.0
@export var flask_max: int = 3          ## Frascos de Essência (§3.2)
@export var flask_heal_ratio: float = 0.4

enum State { FREE, ATTACKING, DODGING, GUARDING, STUNNED, GRAPPLING }
var state: State = State.FREE

@onready var health: HealthComponent = $HealthComponent
@onready var stamina: StaminaComponent = $StaminaComponent
@onready var hurtbox: Hurtbox = $Hurtbox
@onready var lock_on: LockOnSystem = $LockOnSystem
@onready var attack_hitbox: Hitbox = $AttackHitbox
@onready var interaction_detector: Area2D = $InteractionDetector
@onready var body_visual: Polygon2D = $Visual
@onready var camera: Camera2D = $Camera2D
## Opcional: se um AnimatedSprite2D "Sprite" for adicionado (swap de arte —
## Ninja Adventure CC0), ele é dirigido pelo estado. Ausente = greybox, sem efeito.
@onready var sprite: AnimatedSprite2D = get_node_or_null("Sprite")

const WEAPON_PATHS := [
	"res://data/weapons/espada_guardia.tres",
	"res://data/weapons/adaga_dupla.tres",
]

var _facing: Vector2 = Vector2.DOWN
var _dodge_timer: float = 0.0
var _attack_timer: float = 0.0
var flasks: int = 0
var _weapons: Array[WeaponData] = []
var _weapon_index: int = 0
var equipped_weapon: WeaponData
const COMBO_WINDOW := 0.55
var _combo_index: int = 0
var _combo_timer: float = 0.0
var _shake_time: float = 0.0

func _ready() -> void:
	add_to_group("player")
	health.is_player = true
	health.died.connect(_on_died)
	hurtbox.hit_taken.connect(_on_hurt)
	GameEvents.parry_success.connect(_on_parry_success)
	flasks = flask_max
	GameEvents.flasks_changed.emit(flasks, flask_max)
	apply_attributes()
	# Adiado: o HUD (último filho da cena) precisa estar conectado ao weapon_changed.
	_load_weapons.call_deferred()

func _load_weapons() -> void:
	for p in WEAPON_PATHS:
		var w := load(p) as WeaponData
		if w:
			_weapons.append(w)
	if not _weapons.is_empty():
		equip(0)

## Equipa a arma de índice `i` — cada arma muda dano/postura/custo/velocidade (§3.3).
func equip(i: int) -> void:
	if i < 0 or i >= _weapons.size():
		return
	_weapon_index = i
	equipped_weapon = _weapons[i]
	GameEvents.weapon_changed.emit(equipped_weapon.display_name)

func swap_weapon() -> void:
	if _weapons.size() > 1:
		equip((_weapon_index + 1) % _weapons.size())

## Aplica os atributos salvos aos stats derivados (§3.3). Chamado ao nascer e
## sempre que Aria sobe um atributo no santuário.
func apply_attributes() -> void:
	var attrs: Dictionary = SaveManager.state["attributes"]
	health.set_max_health(Attributes.max_hp_for(int(attrs["vitalidade"])))
	stamina.set_max(Attributes.max_stamina_for(int(attrs["stamina"])))

func _physics_process(delta: float) -> void:
	_poll_actions()
	if _combo_timer > 0.0:
		_combo_timer -= delta
		if _combo_timer <= 0.0:
			_combo_index = 0  ## janela expirou: combo reinicia
	match state:
		State.DODGING:
			_process_dodge(delta)
		State.ATTACKING:
			_process_attack(delta)
		State.GRAPPLING:
			velocity = Vector2.ZERO  ## movimento via tween do gancho
		_:
			_process_free()
	move_and_slide()
	_update_visual()
	_apply_shake(delta)

## Feedback de dano: flash no corpo + tremor de câmera + faísca (§5).
func _on_hurt(_hitbox: Hitbox) -> void:
	body_visual.modulate = Color(4, 4, 4)
	create_tween().tween_property(body_visual, "modulate", Color(1, 1, 1), 0.15)
	_shake_time = 0.18
	CombatFx.spark(self)

func _apply_shake(delta: float) -> void:
	if _shake_time > 0.0:
		_shake_time -= delta
		camera.offset = Vector2(randf_range(-2.5, 2.5), randf_range(-2.5, 2.5))
	elif camera.offset != Vector2.ZERO:
		camera.offset = Vector2.ZERO

## Dirige o AnimatedSprite2D opcional pelo estado/facing (pronto para o swap de
## arte). No greybox, `sprite` é null e este método é um no-op.
func _update_visual() -> void:
	if sprite == null or sprite.sprite_frames == null:
		return
	var anim := "idle"
	match state:
		State.DODGING: anim = "dodge"
		State.ATTACKING: anim = "attack"
		State.STUNNED: anim = "hurt"
		_: anim = "walk" if velocity.length() > 5.0 else "idle"
	if sprite.sprite_frames.has_animation(anim):
		sprite.play(anim)
	if absf(_facing.x) > 0.01:
		sprite.flip_h = _facing.x < 0.0

## Polling das ações (não _unhandled_input): assim os botões touch do HUD, que
## disparam Input.action_press/release, acionam as mesmas ações que teclado/gamepad.
func _poll_actions() -> void:
	if state == State.STUNNED or state == State.GRAPPLING:
		return
	# Interagir funciona mesmo travado? Não: o diálogo consome o input à parte.
	if GameConfig.gameplay_locked:
		return
	if Input.is_action_just_pressed("interact"):
		_try_interact()
	if Input.is_action_just_pressed("heal"):
		use_flask()
	if Input.is_action_just_pressed("swap_weapon"):
		swap_weapon()
	if Input.is_action_just_pressed("lock_on"):
		lock_on.toggle()
	if Input.is_action_just_pressed("attack"):
		_try_attack()
	if Input.is_action_just_pressed("dodge"):
		_try_dodge()
	if Input.is_action_just_pressed("guard"):
		_set_guard(true)
	elif Input.is_action_just_released("guard"):
		_set_guard(false)

## Toque direto num inimigo troca o alvo de lock-on (§3.1) — precisa da posição
## do toque, então continua vindo do evento real.
func _unhandled_input(event: InputEvent) -> void:
	if event is InputEventScreenTouch and event.pressed:
		_try_touch_target(event.position)

func _process_free() -> void:
	if GameConfig.gameplay_locked:
		velocity = Vector2.ZERO
		return
	var dir := Input.get_vector("move_left", "move_right", "move_up", "move_down")
	velocity = dir * speed
	if dir != Vector2.ZERO:
		_facing = dir.normalized()

func _process_dodge(delta: float) -> void:
	_dodge_timer -= delta
	if _dodge_timer <= dodge_duration - dodge_iframes:
		hurtbox.invulnerable = false  ## fim dos i-frames
	if _dodge_timer <= 0.0:
		state = State.FREE
		hurtbox.invulnerable = false

func _process_attack(delta: float) -> void:
	velocity = velocity.move_toward(Vector2.ZERO, speed * 8.0 * delta)
	_attack_timer -= delta
	if _attack_timer <= 0.0:
		attack_hitbox.deactivate()
		state = State.FREE

func _try_attack() -> void:
	if state != State.FREE:
		return
	# Custo/dano/velocidade vêm da arma equipada (§3.3); fallback se não houver.
	var cost := equipped_weapon.stamina_cost if equipped_weapon else attack_stamina
	if not stamina.try_spend(cost):
		return  ## sem stamina = não ataca (§3.2)
	state = State.ATTACKING
	# Encadeia o combo: cada golpe na janela avança até combo_length; o golpe
	# final é um finalizador (mais dano de postura, §3.2/§3.3).
	var combo_len := equipped_weapon.combo_length if equipped_weapon else 3
	if _combo_timer > 0.0 and _combo_index < combo_len:
		_combo_index += 1
	else:
		_combo_index = 1
	_combo_timer = COMBO_WINDOW
	var is_finisher := _combo_index >= combo_len
	if equipped_weapon:
		var dmg_mult := 1.0 + 0.15 * (_combo_index - 1)
		attack_hitbox.damage = equipped_weapon.base_damage * dmg_mult
		attack_hitbox.poise_damage = equipped_weapon.poise_damage * (1.6 if is_finisher else 1.0)
		_attack_timer = 0.35 / maxf(equipped_weapon.attack_speed, 0.1)
	else:
		_attack_timer = 0.35
	# Direciona o hitbox para o alvo travado, se houver; senão para o facing.
	var aim := _facing
	if lock_on.current_target:
		aim = (lock_on.current_target.global_position - global_position).normalized()
		_facing = aim
	attack_hitbox.position = aim * 18.0
	attack_hitbox.activate()

func _try_dodge() -> void:
	if state == State.ATTACKING or state == State.STUNNED:
		return
	if not stamina.try_spend(dodge_stamina):
		return
	state = State.DODGING
	_dodge_timer = dodge_duration
	hurtbox.invulnerable = true  ## i-frames começam
	var dir := Input.get_vector("move_left", "move_right", "move_up", "move_down")
	velocity = (dir if dir != Vector2.ZERO else _facing) * dodge_speed

func _set_guard(active: bool) -> void:
	if active and state == State.FREE:
		state = State.GUARDING
		hurtbox.guarding = true
		hurtbox.parry_active = true
		# Janela de parry conforme dificuldade (§3.4).
		var window: float = GameConfig.current_rules()["parry_window"]
		get_tree().create_timer(window).timeout.connect(func(): hurtbox.parry_active = false)
	elif not active:
		hurtbox.guarding = false
		hurtbox.parry_active = false
		if state == State.GUARDING:
			state = State.FREE

## Interage com o Interactable mais próximo dentro do alcance (§3.1).
func _try_interact() -> void:
	var best: Interactable = null
	var best_d := INF
	for a in interaction_detector.get_overlapping_areas():
		if a is Interactable:
			var d := global_position.distance_squared_to((a as Node2D).global_position)
			if d < best_d:
				best_d = d
				best = a
	if best:
		best.interact(self)

## Frasco de Essência: cura limitada, recarregável nos santuários (§3.2).
func use_flask() -> void:
	if flasks <= 0 or state == State.STUNNED:
		return
	flasks -= 1
	health.heal(health.max_health * flask_heal_ratio)
	GameEvents.flasks_changed.emit(flasks, flask_max)

## Restaura tudo ao descansar num santuário (§3.2).
func full_restore() -> void:
	health.heal(health.max_health)
	flasks = flask_max
	GameEvents.flasks_changed.emit(flasks, flask_max)

## Gancho-corda (§3.3): puxa Aria através de um abismo até o destino, com i-frames.
func grapple_to(target: Vector2) -> void:
	if state == State.GRAPPLING:
		return
	state = State.GRAPPLING
	hurtbox.invulnerable = true
	var t := create_tween()
	t.tween_property(self, "global_position", target, 0.35).set_trans(Tween.TRANS_SINE)
	t.tween_callback(_end_grapple)

func _end_grapple() -> void:
	hurtbox.invulnerable = false
	state = State.FREE

## Renasce no santuário após a morte (chamado pelo GameWorld).
func revive() -> void:
	state = State.FREE
	hurtbox.invulnerable = false
	hurtbox.guarding = false
	hurtbox.parry_active = false
	health.recover_poise()
	full_restore()
	set_physics_process(true)

func _try_touch_target(screen_pos: Vector2) -> void:
	var world := get_global_mouse_position() if screen_pos == Vector2.ZERO \
		else get_canvas_transform().affine_inverse() * screen_pos
	var best: Node2D = null
	var best_d := 40.0  ## raio de toque em pixels de mundo
	for e in get_tree().get_nodes_in_group("enemies"):
		if e is Node2D:
			var d := (e as Node2D).global_position.distance_to(world)
			if d < best_d:
				best_d = d
				best = e
	if best:
		lock_on.set_target(best)

func _on_parry_success(target: Node) -> void:
	# Placeholder: em produção dispara animação de crítico/finalização.
	if target and target.has_method("stagger"):
		target.stagger()

func _on_died() -> void:
	# "Morte com peso": dropa os Ecos no local (§3.2). Regras por dificuldade.
	# Em Balada mantém-se os Ecos, então nada é dropado (dropped = 0).
	var dropped := 0
	if not GameConfig.current_rules()["keep_ecos"]:
		dropped = SaveManager.state["aria"]["ecos"]
		SaveManager.state["aria"]["ecos"] = 0
		GameEvents.ecos_changed.emit(0)
	GameEvents.player_died.emit(global_position, dropped)
	set_physics_process(false)
