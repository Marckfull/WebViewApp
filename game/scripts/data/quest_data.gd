class_name QuestData
extends Resource
## QuestData — side quest data-driven (§3.5). Fase 1 cobre o objetivo de caça
## (matar N de um inimigo); outros tipos (coletar, escoltar) entram depois.

enum Objective { KILL }

@export var id: StringName
@export var title: String = "Missão"
@export var objective: Objective = Objective.KILL
@export var target: StringName          ## id do inimigo (para KILL)
@export var count: int = 1
@export var reward_ecos: int = 100
