extends Level
## Necrópole de Sal: Carcaças entre correntes de sal e a Rainha de Sal,
## que guarda as Botas de Corrente. Uma corrente esconde uma Memória
## que só se alcança depois de conquistar as Botas.

const RESOURCE_NODE := preload("res://world/resource_node.tscn")
const BOTAS_POSITION := Vector2(400, 110)

@onready var boss: BossEcoGuardia = $Boss
@onready var trigger: Area2D = $BossTrigger


func _ready() -> void:
	super()
	GameEvents.boss_ended.connect(_on_boss_ended)
	GameEvents.shrine_rested.connect(_on_rest)
	trigger.body_entered.connect(_on_trigger_entered)
	if GameState.flags.get("necropole_boss_derrotado", false):
		boss.queue_free()
		trigger.queue_free()
		_spawn_botas()


func _on_trigger_entered(body: Node2D) -> void:
	if body is Player and body.is_alive() \
			and not boss.is_engaged() and not boss.is_defeated():
		boss.activate()


func _on_boss_ended(victory: bool) -> void:
	if victory:
		GameState.flags["necropole_boss_derrotado"] = true
		_spawn_botas()


## Recompensa da dungeon: as Botas de Corrente.
func _spawn_botas() -> void:
	if GameState.flags.get("coletado_botas_corrente", false):
		return
	var node := RESOURCE_NODE.instantiate()
	node.item_id = "botas_corrente"
	node.one_time = true
	node.position = BOTAS_POSITION
	add_child.call_deferred(node)


func _on_rest() -> void:
	if is_instance_valid(boss):
		boss.reset()
