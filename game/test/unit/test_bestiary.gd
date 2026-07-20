extends "res://addons/gut/test.gd"
## Testes do bestiário automático (§3.6, §6.3).

func test_record_new_entry_returns_true() -> void:
	var entries: Array = []
	assert_true(Bestiary.record(&"ecoado_comum", entries))
	assert_eq(entries.size(), 1)

func test_record_duplicate_returns_false() -> void:
	var entries: Array = ["ecoado_comum"]
	assert_false(Bestiary.record(&"ecoado_comum", entries))
	assert_eq(entries.size(), 1)

func test_record_empty_id_ignored() -> void:
	var entries: Array = []
	assert_false(Bestiary.record(&"", entries))
	assert_eq(entries.size(), 0)

func test_has_entry() -> void:
	var entries: Array = ["guardia_do_eco"]
	assert_true(Bestiary.has_entry(&"guardia_do_eco", entries))
	assert_false(Bestiary.has_entry(&"ecoado_comum", entries))

func test_count() -> void:
	assert_eq(Bestiary.count(["a", "b", "c"]), 3)
