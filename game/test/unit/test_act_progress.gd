extends "res://addons/gut/test.gd"
## Testes do progresso do Ato 2 (§2): a descida ao Coração Mudo só libera com os
## 4 bosses de dungeon derrotados.

func test_none_cleared() -> void:
	assert_eq(ActProgress.cleared_count([]), 0)
	assert_false(ActProgress.all_cleared([]))
	assert_eq(ActProgress.remaining([]), 4)

func test_partial_does_not_open() -> void:
	var defeated := ["coro_enraizado", "martelo_mudo"]
	assert_eq(ActProgress.cleared_count(defeated), 2)
	assert_false(ActProgress.all_cleared(defeated))
	assert_eq(ActProgress.remaining(defeated), 2)

func test_other_bosses_do_not_count() -> void:
	# Guardiã do Eco e Selene não são dungeons do Ato 2.
	var defeated := ["guardia_do_eco", "selene"]
	assert_eq(ActProgress.cleared_count(defeated), 0)
	assert_false(ActProgress.all_cleared(defeated))

func test_all_four_open_the_way() -> void:
	var defeated := ["coro_enraizado", "martelo_mudo", "sino_invertido", "mare_salgada"]
	assert_true(ActProgress.all_cleared(defeated))
	assert_eq(ActProgress.remaining(defeated), 0)
