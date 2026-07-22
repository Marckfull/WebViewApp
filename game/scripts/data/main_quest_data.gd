class_name MainQuestData
extends Resource
## MainQuestData — missão principal com etapas rastreadas (§3.5). Cada etapa é
## concluída por um evento real do jogo: obter um item (obtain), derrotar um chefe
## (kill_boss) ou abater um inimigo (kill). Ao concluir a última etapa, ativa a
## `next_quest`, encadeando a campanha do Ato 2. Arrays paralelos por etapa.

@export var id: StringName
@export var title: String = "Missão"
@export var next_quest: StringName = &""          ## próxima missão da campanha (ou vazio)
@export var step_descs: Array[String] = []        ## objetivo de cada etapa
@export var step_kinds: Array[String] = []        ## "obtain" | "kill_boss" | "kill"
@export var step_targets: Array[StringName] = []  ## id do item/chefe/inimigo

func step_count() -> int:
	var n := step_descs.size()
	n = mini(n, step_kinds.size())
	n = mini(n, step_targets.size())
	return n
