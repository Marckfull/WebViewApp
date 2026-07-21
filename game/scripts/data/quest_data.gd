class_name QuestData
extends Resource
## QuestData — side quest data-driven (§3.5). Objetivos: KILL (matar N de um
## inimigo) e COLLECT (entregar N de um recurso). Escoltar entra depois.

enum Objective { KILL, COLLECT }

@export var id: StringName
@export var title: String = "Missão"
@export var objective: Objective = Objective.KILL
@export var target: StringName          ## id do inimigo (KILL) ou do recurso (COLLECT)
@export var count: int = 1
@export var reward_ecos: int = 100
