extends "res://addons/gut/test.gd"
## Testes do sistema de cutscene (§2/§6.3). A lógica de "vista" reusa Inventory
## (já testado); aqui garantimos os dados e o gating por lista.

func test_cutscene_data_loads() -> void:
	var c: CutsceneData = load("res://data/cutscenes/boss_approach.tres")
	assert_not_null(c)
	assert_eq(c.id, &"boss_approach")
	assert_gt(c.pages.size(), 0)

func test_seen_gating_via_inventory() -> void:
	var seen: Array = []
	assert_false(Inventory.has(&"boss_approach", seen))
	Inventory.grant(&"boss_approach", seen)
	assert_true(Inventory.has(&"boss_approach", seen))
