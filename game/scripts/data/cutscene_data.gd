class_name CutsceneData
extends Resource
## Cutscene narrada data-driven (§2/§4). Fase greybox: páginas de texto (sem arte).
## Quando houver ilustrações, cada página pode ganhar uma textura de fundo.

@export var id: StringName
@export_multiline var pages: Array[String] = []
