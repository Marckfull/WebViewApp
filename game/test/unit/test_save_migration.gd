extends "res://addons/gut/test.gd"
## Testes da migração de save (§6.2/§6.3): preenche chaves novas sem perder dados.

func test_fills_missing_top_level_key() -> void:
	var data: Dictionary = {"aria": {"ecos": 5}}
	var defaults: Dictionary = {"aria": {"ecos": 0}, "items": []}
	SaveMigration.merge_defaults(data, defaults)
	assert_true(data.has("items"))

func test_preserves_existing_values() -> void:
	var data: Dictionary = {"aria": {"ecos": 99}}
	var defaults: Dictionary = {"aria": {"ecos": 0, "hp": 100.0}}
	SaveMigration.merge_defaults(data, defaults)
	assert_eq(int(data["aria"]["ecos"]), 99, "valor do jogador é preservado")
	assert_eq(data["aria"]["hp"], 100.0, "chave nova é adicionada")

func test_merges_nested_dicts() -> void:
	var data: Dictionary = {"world": {"melodies": ["cancao_do_mundo"]}}
	var defaults: Dictionary = {"world": {"melodies": [], "walls_broken": [], "map": {}}}
	SaveMigration.merge_defaults(data, defaults)
	assert_eq(data["world"]["melodies"].size(), 1, "lista existente preservada")
	assert_true(data["world"].has("walls_broken"))
	assert_true(data["world"].has("map"))

func test_copied_defaults_are_independent() -> void:
	var defaults: Dictionary = {"items": []}
	var data: Dictionary = {}
	SaveMigration.merge_defaults(data, defaults)
	data["items"].append("gancho")
	assert_eq(defaults["items"].size(), 0, "mutar o save não afeta o default")
