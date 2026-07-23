extends Node
## QuestManager — acompanha o progresso das side quests (autoload, §3.5).
## Carrega o registro de QuestData e atualiza o progresso via eventos (mortes).

const QUEST_PATHS := [
	"res://data/quests/matar_couracado.tres",
	"res://data/quests/silenciar_arqueiros.tres",
	"res://data/quests/cacar_noturno.tres",
	"res://data/quests/entregar_minerio.tres",
	"res://data/quests/escoltar_ferido.tres",
	"res://data/quests/cao_sem_nome.tres",
	"res://data/quests/ervas_alquimista.tres",
]

var _quests: Dictionary = {}  # id(String) -> QuestData

func _ready() -> void:
	for p in QUEST_PATHS:
		var q := load(p) as QuestData
		if q:
			_quests[String(q.id)] = q
	GameEvents.enemy_defeated.connect(_on_enemy_defeated)
	GameEvents.escort_reached.connect(_on_escort_reached)

func start(quest: QuestData) -> void:
	SaveManager.state["quests"][String(quest.id)] = {"status": Quest.ACTIVE, "progress": 0}
	SaveManager.save_game()

## Quanto Aria tem do recurso-alvo de uma quest COLLECT (bolsa de recursos).
func collect_count(quest: QuestData) -> int:
	return Consumables.count(quest.target, SaveManager.state["resources"])

## Uma quest está pronta para entregar? (KILL por progresso; COLLECT por bolsa.)
func is_ready(quest: QuestData) -> bool:
	var quests: Dictionary = SaveManager.state["quests"]
	if quest.objective == QuestData.Objective.COLLECT:
		return Quest.is_collect_ready(quest, quests, collect_count(quest))
	return Quest.is_ready_to_complete(quest, quests)

func turn_in(quest: QuestData) -> bool:
	if not is_ready(quest):
		return false
	# COLLECT consome os recursos entregues (delivery).
	if quest.objective == QuestData.Objective.COLLECT:
		var bag: Dictionary = SaveManager.state["resources"]
		for i in quest.count:
			Consumables.consume(quest.target, bag)
		GameEvents.consumable_changed.emit(quest.target, Consumables.count(quest.target, bag))
	SaveManager.state["aria"]["ecos"] += quest.reward_ecos
	GameEvents.ecos_changed.emit(SaveManager.state["aria"]["ecos"])
	SaveManager.state["quests"][String(quest.id)]["status"] = Quest.COMPLETE
	SaveManager.save_game()
	return true

## O NPC escoltado chegou: conclui a quest ESCORT correspondente (§3.5).
func _on_escort_reached(escort_id: StringName) -> void:
	var quests: Dictionary = SaveManager.state["quests"]
	for key in quests:
		var e: Variant = quests[key]
		if not (e is Dictionary) or e.get("status") != Quest.ACTIVE:
			continue
		var qd: QuestData = _quests.get(key)
		if qd and qd.objective == QuestData.Objective.ESCORT and String(qd.target) == String(escort_id):
			e["progress"] = qd.count
			turn_in(qd)  ## conclusão automática ao alcançar o destino
			return

func _on_enemy_defeated(enemy_id: StringName, _pos: Vector2) -> void:
	var quests: Dictionary = SaveManager.state["quests"]
	var changed := false
	for key in quests:
		var e: Variant = quests[key]
		if not (e is Dictionary) or e.get("status") != Quest.ACTIVE:
			continue
		var qd: QuestData = _quests.get(key)
		if qd and qd.objective == QuestData.Objective.KILL and String(qd.target) == String(enemy_id):
			e["progress"] = int(e.get("progress", 0)) + 1
			changed = true
	if changed:
		SaveManager.save_game()
