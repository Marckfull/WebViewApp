class_name SaveMigration
extends RefCounted
## Migração de save robusta (§6.2). Saves antigos podem não ter chaves adicionadas
## em versões novas; mesclar os defaults evita KeyError e mantém os dados do jogador.
## Lógica pura e testável.

## Preenche recursivamente, em `data`, as chaves ausentes vindas de `defaults`.
## Valores já presentes são preservados; dicionários aninhados são mesclados.
static func merge_defaults(data: Dictionary, defaults: Dictionary) -> Dictionary:
	for key in defaults:
		if not data.has(key):
			data[key] = _copy(defaults[key])
		elif typeof(defaults[key]) == TYPE_DICTIONARY and typeof(data[key]) == TYPE_DICTIONARY:
			merge_defaults(data[key], defaults[key])
	return data

static func _copy(value: Variant) -> Variant:
	if value is Dictionary or value is Array:
		return value.duplicate(true)
	return value
