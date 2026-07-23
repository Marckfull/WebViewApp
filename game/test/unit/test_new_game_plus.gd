extends "res://addons/gut/test.gd"
## Testes do New Game+ (§3.6): multiplicadores e reset de run.

func test_multipliers_grow_with_cycle() -> void:
	assert_almost_eq(NewGamePlus.health_mult(0), 1.0, 0.001)
	assert_gt(NewGamePlus.health_mult(2), NewGamePlus.health_mult(1))
	assert_gt(NewGamePlus.damage_mult(1), NewGamePlus.damage_mult(0))

func test_reset_increments_cycle() -> void:
	var state := {"ng_cycle": 0, "world": {}, "quests": {"q": {}}}
	NewGamePlus.reset_run(state)
	assert_eq(int(state["ng_cycle"]), 1)

func test_reset_clears_world_run_state() -> void:
	var state := {
		"world": {"bosses_defeated": ["guardia_do_eco"], "walls_broken": ["p"],
			"eco_drop": {"amount": 9}, "map": {"a": 1}},
		"quests": {"q": {"status": "complete"}},
	}
	NewGamePlus.reset_run(state)
	assert_eq((state["world"]["bosses_defeated"] as Array).size(), 0)
	assert_eq((state["world"]["walls_broken"] as Array).size(), 0)
	assert_true((state["world"]["eco_drop"] as Dictionary).is_empty())
	assert_true((state["quests"] as Dictionary).is_empty())

func test_reset_keeps_progression() -> void:
	var state := {
		"ng_cycle": 0, "world": {},
		"attributes": {"vitalidade": 5}, "items": ["gancho", "bomba"],
		"weapon_levels": {"espada_guardia": 3},
	}
	NewGamePlus.reset_run(state)
	assert_eq(int(state["attributes"]["vitalidade"]), 5, "atributos mantidos")
	assert_eq((state["items"] as Array).size(), 2, "itens mantidos")
	assert_eq(int(state["weapon_levels"]["espada_guardia"]), 3, "níveis de arma mantidos")
