extends Level
## Cripta das Guardiãs: corredor de Ecoados e a luta contra o Eco da
## Guardiã. O portão fecha ao entrar na sala; o boss reseta se a
## jogadora morrer ou descansar; a vitória fica gravada em GameState.

const RESOURCE_NODE := preload("res://world/resource_node.tscn")
const GANCHO_POSITION := Vector2(320, 140)

static var ACT2_CUTSCENE := PackedStringArray([
	"A Guardiã cai, e da névoa surge uma Ocarina de Vidro — o instrumento "
			+ "das Guardiãs, que só quem ouve a Canção pode tocar.",
	"Aria entende, enfim: o Silêncio se prende a quatro Santuários do Eco "
			+ "espalhados por Lirael — Floresta, Forja, Torre e Necrópole.",
	"Restaure os quatro, e o caminho até o coração do Silêncio se abrirá. "
			+ "Só então ela reencontrará Lys.",
])

@onready var boss: BossEcoGuardia = $Boss
@onready var gate_shape: CollisionShape2D = $Gate/CollisionShape2D
@onready var gate_visual: Polygon2D = $Gate/Visual
@onready var trigger: Area2D = $BossTrigger


func _ready() -> void:
	super()
	GameEvents.boss_ended.connect(_on_boss_ended)
	GameEvents.shrine_rested.connect(_on_rest)
	trigger.body_entered.connect(_on_trigger_entered)
	if GameState.flags.get("cripta_boss_derrotado", false):
		boss.queue_free()
		trigger.queue_free()
		_spawn_gancho()


func _on_trigger_entered(body: Node2D) -> void:
	if body is Player and body.is_alive() \
			and not boss.is_engaged() and not boss.is_defeated():
		boss.activate()
		_set_gate(true)


func _set_gate(closed: bool) -> void:
	gate_visual.visible = closed
	gate_shape.set_deferred("disabled", not closed)


func _on_boss_ended(victory: bool) -> void:
	if victory:
		GameState.flags["cripta_boss_derrotado"] = true
		_spawn_gancho()
		if not GameState.flags.get("cutscene_act2", false):
			GameState.flags["cutscene_act2"] = true
			_play_act2_cutscene()
	_set_gate(false)


func _play_act2_cutscene() -> void:
	await get_tree().create_timer(2.2).timeout
	GameState.pending_cutscene = ACT2_CUTSCENE
	GameState.cutscene_return = "res://world/crypt.tscn"
	GameState.next_spawn = "default"
	get_tree().change_scene_to_file("res://ui/cutscene.tscn")


## Recompensa da dungeon: o Gancho-corda surge onde a Guardiã caiu.
func _spawn_gancho() -> void:
	if GameState.flags.get("coletado_gancho_corda", false):
		return
	var node := RESOURCE_NODE.instantiate()
	node.item_id = "gancho_corda"
	node.one_time = true
	node.position = GANCHO_POSITION
	add_child.call_deferred(node)


func _on_rest() -> void:
	if is_instance_valid(boss):
		boss.reset()
