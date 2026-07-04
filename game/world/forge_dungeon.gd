extends Level
## Forja Afundada: Forjados blindados entre poças d'água e o Coração
## da Forja guardando a Bomba de Eco. O boss reseta ao morrer/descansar.

const RESOURCE_NODE := preload("res://world/resource_node.tscn")
const BOMB_POSITION := Vector2(400, 110)

@onready var boss: BossEcoGuardia = $Boss
@onready var trigger: Area2D = $BossTrigger


func _ready() -> void:
	super()
	GameEvents.boss_ended.connect(_on_boss_ended)
	GameEvents.shrine_rested.connect(_on_rest)
	trigger.body_entered.connect(_on_trigger_entered)
	if GameState.flags.get("forja_boss_derrotado", false):
		boss.queue_free()
		trigger.queue_free()
		_spawn_bomb_item()


func _on_trigger_entered(body: Node2D) -> void:
	if body is Player and body.is_alive() \
			and not boss.is_engaged() and not boss.is_defeated():
		boss.activate()


func _on_boss_ended(victory: bool) -> void:
	if victory:
		GameState.flags["forja_boss_derrotado"] = true
		_spawn_bomb_item()


## Recompensa da dungeon: a Bomba de Eco nasce do coração apagado.
func _spawn_bomb_item() -> void:
	if GameState.flags.get("coletado_bomba_eco", false):
		return
	var node := RESOURCE_NODE.instantiate()
	node.item_id = "bomba_eco"
	node.one_time = true
	node.position = BOMB_POSITION
	add_child.call_deferred(node)


func _on_rest() -> void:
	if is_instance_valid(boss):
		boss.reset()
