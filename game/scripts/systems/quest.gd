class_name Quest
extends RefCounted
## Lógica pura de estado de quests (§3.5/§6.3). O save guarda um dicionário
## id -> { status, progress }. Sem UI/IO.

const UNSTARTED := "unstarted"
const ACTIVE := "active"
const COMPLETE := "complete"

static func status(quests: Dictionary, id: StringName) -> String:
	var e: Variant = quests.get(String(id), null)
	return e["status"] if e is Dictionary and e.has("status") else UNSTARTED

static func progress(quests: Dictionary, id: StringName) -> int:
	var e: Variant = quests.get(String(id), null)
	return int(e["progress"]) if e is Dictionary and e.has("progress") else 0

static func is_ready_to_complete(quest: QuestData, quests: Dictionary) -> bool:
	return status(quests, quest.id) == ACTIVE and progress(quests, quest.id) >= quest.count

## Para COLLECT o "progresso" é o que Aria tem agora na bolsa (`have`), não um
## contador acumulado — pura, para o giver/manager checarem sem tocar no save.
static func is_collect_ready(quest: QuestData, quests: Dictionary, have: int) -> bool:
	return status(quests, quest.id) == ACTIVE and have >= quest.count
