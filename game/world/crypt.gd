extends Level
## Cripta das Guardiãs: corredor de Ecoados e a luta contra o Eco da
## Guardiã. O portão fecha ao entrar na sala; o boss reseta se a
## jogadora morrer ou descansar; a vitória fica gravada em GameState.

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
	_set_gate(false)


func _on_rest() -> void:
	if is_instance_valid(boss):
		boss.reset()
