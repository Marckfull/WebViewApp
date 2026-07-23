extends "res://addons/gut/test.gd"
## Testes da reviravolta do Ato 2 (§2): a cutscene existe e só é elegível quando
## os 4 Santuários foram restaurados (reusa a lógica de ActProgress).

func test_cutscene_loads() -> void:
	var c: CutsceneData = load("res://data/cutscenes/reviravolta_ato2.tres")
	assert_not_null(c)
	assert_eq(c.id, &"reviravolta_ato2")
	assert_gt(c.pages.size(), 0)

func test_eligibility_follows_act_progress() -> void:
	# Sem os 4 bosses, a reviravolta não dispara.
	assert_false(ActProgress.all_cleared([]))
	assert_false(ActProgress.all_cleared(["coro_enraizado", "martelo_mudo", "sino_invertido"]))
	# Com os 4, dispara.
	assert_true(ActProgress.all_cleared(
		["coro_enraizado", "martelo_mudo", "sino_invertido", "mare_salgada"]))
