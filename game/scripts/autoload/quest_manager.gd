extends Node
## QuestManager — acompanha o progresso das side quests (autoload, §3.5).
## Carrega o registro de QuestData e atualiza o progresso via eventos (mortes).

const QUEST_PATHS := [
	"res://data/quests/matar_couracado.tres",
	"res://data/quests/silenciar_arqueiros.tres",
	"res://data/quests/cacar_noturno.tres",
]

var _quests: Dictionary = {}  # id(String) -> QuestData

func _ready() -> void:
	for p in QUEST_PATHS:
		var q := load(p) as QuestData
		if q:
			_quests[String(q.id)] = q
	GameEvents.enemy_defeated.connect(_on_enemy_defeated)

func start(quest: QuestData) -> void:
	SaveManager.state["quests"][String(quest.id)] = {"status": Quest.ACTIVE, "progress": 0}
	SaveManager.save_game()

func turn_in(quest: QuestData) -> bool:
	var quests: Dictionary = SaveManager.state["quests"]
	if not Quest.is_ready_to_complete(quest, quests):
		return false
	SaveManager.state["aria"]["ecos"] += quest.reward_ecos
	GameEvents.ecos_changed.emit(SaveManager.state["aria"]["ecos"])
	quests[String(quest.id)]["status"] = Quest.COMPLETE
	SaveManager.save_game()
	return true

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
