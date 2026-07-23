extends CharacterBody2D
## EscortNpc — NPC guiado numa missão ESCORT (§3.5). Segue o jogador enquanto a
## quest está ativa, mas só se aproxima até uma distância (o jogador precisa
## liderar, não sprintar). Ao entrar na zona de destino (EscortGoal.arrive),
## conclui a escolta. Fica no grupo "escort" para o destino detectá-lo.

@export var quest: QuestData
@export var escort_id: StringName = &"escort"
@export var speed: float = 55.0
@export var leash_min: float = 40.0     ## para de seguir quando já está perto
@export var arrival_dialogue: DialogueData

var _done: bool = false

func _ready() -> void:
	add_to_group("escort")

func _is_active() -> bool:
	return quest != null and Quest.status(SaveManager.state["quests"], quest.id) == Quest.ACTIVE

func _physics_process(_delta: float) -> void:
	if _done or not _is_active():
		velocity = Vector2.ZERO
		return
	var p := get_tree().get_first_node_in_group("player") as Node2D
	if p == null:
		velocity = Vector2.ZERO
		return
	var to := p.global_position - global_position
	velocity = to.normalized() * speed if to.length() > leash_min else Vector2.ZERO
	move_and_slide()

## Chamado pelo EscortGoal quando o NPC entra no destino.
func arrive() -> void:
	if _done or not _is_active():
		return
	_done = true
	velocity = Vector2.ZERO
	GameEvents.escort_reached.emit(escort_id)
	if arrival_dialogue:
		DialogueManager.start(arrival_dialogue)
