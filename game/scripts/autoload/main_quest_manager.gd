extends Node
## MainQuestManager — campanha do Ato 2 com etapas rastreadas (autoload, §3.5).
##
## As 4 missões principais (uma por dungeon) avançam em PARALELO — o hub é não
## linear, então limpar as dungeons em qualquer ordem funciona. Cada missão tem
## etapas concluídas por eventos reais (obter item / abater chefe / abater inimigo).
## O objetivo atual (o Santuário mais próximo pendente) vai para o HUD.

const IDS := ["mp_floresta", "mp_forja", "mp_torre", "mp_necropole"]
const PATHS := {
	"mp_floresta": "res://data/quests/main/mp_floresta.tres",
	"mp_forja": "res://data/quests/main/mp_forja.tres",
	"mp_torre": "res://data/quests/main/mp_torre.tres",
	"mp_necropole": "res://data/quests/main/mp_necropole.tres",
}

var _quests: Dictionary = {}  # id(String) -> MainQuestData

func _ready() -> void:
	for id in IDS:
		_quests[id] = load(PATHS[id]) as MainQuestData
	GameEvents.item_obtained.connect(_on_item)
	GameEvents.boss_defeated.connect(_on_boss)
	GameEvents.enemy_defeated.connect(_on_enemy)
	# Emite o objetivo inicial após os autoloads/HUD estarem prontos.
	_emit_update.call_deferred()

func _on_item(id: StringName) -> void:
	_advance("obtain", id)

func _on_boss(id: StringName) -> void:
	_advance("kill_boss", id)

func _on_enemy(id: StringName, _pos: Vector2) -> void:
	_advance("kill", id)

func _advance(kind: String, id: StringName) -> void:
	var st: Dictionary = SaveManager.state["main_quest"]
	var steps: Dictionary = st["steps"]
	var completed: Array = st["completed"]
	var changed := false
	for qid in IDS:
		if completed.has(qid):
			continue
		var q: MainQuestData = _quests[qid]
		var step := int(steps.get(qid, 0))
		if MainQuest.step_matches(q, step, kind, id):
			step += 1
			steps[qid] = step
			if MainQuest.is_finished(q, step):
				completed.append(qid)
			changed = true
	if changed:
		SaveManager.save_game()
		_emit_update()

## Objetivo atual: o Santuário mais próximo ainda pendente (ordem IDS).
func current_objective() -> String:
	var st: Dictionary = SaveManager.state["main_quest"]
	var completed: Array = st["completed"]
	var steps: Dictionary = st["steps"]
	for qid in IDS:
		if completed.has(qid):
			continue
		var q: MainQuestData = _quests[qid]
		if q == null:
			continue
		var step := int(steps.get(qid, 0))
		if step < q.step_count():
			return "[%d/4] %s — %s" % [completed.size(), q.title, q.step_descs[step]]
	return "4/4 Santuários restaurados — desça ao Coração Mudo."

func _emit_update() -> void:
	GameEvents.main_quest_updated.emit(current_objective())
