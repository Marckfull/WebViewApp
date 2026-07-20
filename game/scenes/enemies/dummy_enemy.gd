class_name DummyEnemy
extends CharacterBody2D
## Ecoado de treino — inimigo greybox da Fase 0 (a "gym" de combate, §6.3).
##
## Persegue o jogador, aplica dano por contato, quebra postura e morre dropando
## Ecos. Data-driven: stats vêm de um EnemyData (.tres) quando atribuído.

@export var data: EnemyData
@export var move_speed: float = 40.0

@onready var health: HealthComponent = $HealthComponent
@onready var contact_hitbox: Hitbox = $ContactHitbox

var _player: Node2D
var _stagger_timer: float = 0.0

func _ready() -> void:
	add_to_group("enemies")
	if data:
		health.max_health = data.max_health
		health.max_poise = data.max_poise
		move_speed = data.move_speed
		contact_hitbox.damage = data.contact_damage
		health.health = data.max_health
	health.died.connect(_on_died)
	health.poise_broken.connect(_on_poise_broken)
	contact_hitbox.activate()

func _physics_process(delta: float) -> void:
	if _stagger_timer > 0.0:
		_stagger_timer -= delta
		velocity = Vector2.ZERO
		if _stagger_timer <= 0.0:
			health.recover_poise()
		return
	_player = _find_player()
	if _player:
		velocity = (_player.global_position - global_position).normalized() * move_speed
	move_and_slide()

## Chamado pelo parry do jogador (§3.2) — abre janela de finalização.
func stagger() -> void:
	_stagger_timer = 1.5

func _on_poise_broken() -> void:
	stagger()

func _find_player() -> Node2D:
	var players := get_tree().get_nodes_in_group("player")
	return players[0] if not players.is_empty() else null

func _on_died() -> void:
	var reward: int = data.ecos_reward if data else 15
	SaveManager.state["aria"]["ecos"] += reward
	GameEvents.ecos_changed.emit(SaveManager.state["aria"]["ecos"])
	GameEvents.enemy_defeated.emit(data.id if data else &"dummy", global_position)
	queue_free()
