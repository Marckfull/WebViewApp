extends Level
## Torre dos Ventos: correntes que empurram a Aria, Sentinelas que
## atiram rajadas à distância e o Guardião dos Ventos no topo, que
## guarda a Lente da Verdade. O boss reseta ao morrer/descansar.

const RESOURCE_NODE := preload("res://world/resource_node.tscn")
const LENTE_POSITION := Vector2(320, 110)

@onready var boss: BossEcoGuardia = $Boss
@onready var trigger: Area2D = $BossTrigger


func _ready() -> void:
	super()
	GameEvents.boss_ended.connect(_on_boss_ended)
	GameEvents.shrine_rested.connect(_on_rest)
	trigger.body_entered.connect(_on_trigger_entered)
	if GameState.flags.get("torre_boss_derrotado", false):
		boss.queue_free()
		trigger.queue_free()
		_spawn_lente()


func _on_trigger_entered(body: Node2D) -> void:
	if body is Player and body.is_alive() \
			and not boss.is_engaged() and not boss.is_defeated():
		boss.activate()


func _on_boss_ended(victory: bool) -> void:
	if victory:
		GameState.flags["torre_boss_derrotado"] = true
		_spawn_lente()


## Recompensa da dungeon: a Lente da Verdade no ápice da torre.
func _spawn_lente() -> void:
	if GameState.flags.get("coletado_lente_verdade", false):
		return
	var node := RESOURCE_NODE.instantiate()
	node.item_id = "lente_verdade"
	node.one_time = true
	node.position = LENTE_POSITION
	add_child.call_deferred(node)


func _on_rest() -> void:
	if is_instance_valid(boss):
		boss.reset()
