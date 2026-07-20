class_name DialogueData
extends Resource
## DialogueData — diálogo data-driven (§3.5, §6.2). Designers criam .tres sem tocar
## código. Fase 1 assume um falante por diálogo (cobre Corvo, santuários, itens);
## multi-falante e ramificação entram na produção — ou troca-se pelo Dialogue
## Manager (MIT) do catálogo de recursos, mantendo esta interface.

@export var id: StringName
@export var speaker: String = ""
@export_multiline var lines: Array[String] = []
