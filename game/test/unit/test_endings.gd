extends "res://addons/gut/test.gd"
## Testes dos finais do Ato 3 (§2). Lógica pura: disponibilidade (o secreto exige
## as 12 Memórias) e integridade dos textos.

func test_two_endings_without_all_memories() -> void:
	var a := Endings.available(0)
	assert_eq(a.size(), 2)
	assert_true(a.has(Endings.SILENCIAR))
	assert_true(a.has(Endings.COMPLETAR))
	assert_false(a.has(Endings.SECRETO))

func test_eleven_memories_still_no_secret() -> void:
	assert_false(Endings.secret_available(11))
	assert_eq(Endings.available(11).size(), 2)

func test_twelve_memories_unlock_secret() -> void:
	assert_true(Endings.secret_available(12))
	var a := Endings.available(12)
	assert_eq(a.size(), 3)
	assert_true(a.has(Endings.SECRETO))

func test_titles_and_epilogues_present() -> void:
	for e in [Endings.SILENCIAR, Endings.COMPLETAR, Endings.SECRETO]:
		assert_ne(Endings.title(e), "")
		assert_gt(Endings.epilogue(e).length(), 0)

func test_confront_cutscene_loads() -> void:
	var c: CutsceneData = load("res://data/cutscenes/selene_confront.tres")
	assert_not_null(c)
	assert_eq(c.id, &"selene_confront")
	assert_gt(c.pages.size(), 0)
