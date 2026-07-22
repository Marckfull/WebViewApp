class_name QuestData
extends Resource
## QuestData — side quest data-driven (§3.5). Objetivos: KILL (matar N de um
## inimigo), COLLECT (entregar N de um recurso) e ESCORT (guiar um NPC até o
## destino). O `target` identifica o alvo conforme o tipo.

enum Objective { KILL, COLLECT, ESCORT }

@export var id: StringName
@export var title: String = "Missão"
@export var objective: Objective = Objective.KILL
@export var target: StringName          ## id do inimigo (KILL), recurso (COLLECT) ou escolta (ESCORT)
@export var count: int = 1
@export var reward_ecos: int = 100
