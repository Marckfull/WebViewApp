class_name MainQuest
extends RefCounted
## Lógica pura da missão principal (§3.5). Sem estado/IO: decide se um evento do
## jogo conclui a etapa atual. Os eventos mapeiam para "kinds":
##   item_obtained  -> "obtain"
##   boss_defeated  -> "kill_boss"
##   enemy_defeated -> "kill"

## A etapa `index` da missão casa com o evento (ev_kind, ev_id)?
static func step_matches(quest: MainQuestData, index: int, ev_kind: String, ev_id: StringName) -> bool:
	if quest == null or index < 0 or index >= quest.step_count():
		return false
	return quest.step_kinds[index] == ev_kind and String(quest.step_targets[index]) == String(ev_id)

## True se, dado o índice atual, a missão terminou (passou da última etapa).
static func is_finished(quest: MainQuestData, step: int) -> bool:
	return quest != null and step >= quest.step_count()
