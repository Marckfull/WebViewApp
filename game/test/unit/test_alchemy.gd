extends "res://addons/gut/test.gd"
## Testes da alquimia (§3.5/§6.3): ervas -> poções.

func test_can_brew_with_enough() -> void:
	assert_true(Alchemy.can_brew(Alchemy.ERVAS_POR_POCAO))

func test_cannot_brew_without_enough() -> void:
	assert_false(Alchemy.can_brew(Alchemy.ERVAS_POR_POCAO - 1))

func test_brewable_count() -> void:
	assert_eq(Alchemy.brewable(0), 0)
	assert_eq(Alchemy.brewable(Alchemy.ERVAS_POR_POCAO), 1)
	assert_eq(Alchemy.brewable(Alchemy.ERVAS_POR_POCAO * 3 + 1), 3)

func test_recipes_cover_three_herb_tiers() -> void:
	# As três potências de erva devem aparecer como insumo em alguma receita.
	var herbs := {&"erva": false, &"erva_prateada": false, &"erva_do_eco": false}
	for recipe in Alchemy.RECIPES:
		for id in recipe["inputs"]:
			if herbs.has(id):
				herbs[id] = true
	for id in herbs:
		assert_true(herbs[id], "erva '%s' deve alimentar uma receita" % id)

func test_can_make_checks_all_inputs() -> void:
	var elixir: Dictionary = _recipe_for(&"elixir_do_eco")
	assert_not_null(elixir)
	# Falta o cogumelo: não pode.
	assert_false(Alchemy.can_make(elixir, {"erva_do_eco": 5}))
	# Com ambos: pode.
	assert_true(Alchemy.can_make(elixir, {"erva_do_eco": 1, "cogumelo": 1}))

func test_spend_inputs_deducts_exactly() -> void:
	var refeicao: Dictionary = _recipe_for(&"refeicao")
	var bag := {"madeira": 3, "peixe": 2, "inseto": 2}
	assert_true(Alchemy.can_make(refeicao, bag))
	Alchemy.spend_inputs(refeicao, bag)
	assert_eq(int(bag["madeira"]), 2)
	assert_eq(int(bag["peixe"]), 1)
	assert_eq(int(bag["inseto"]), 1)

func test_inputs_text_uses_friendly_names() -> void:
	var elixir: Dictionary = _recipe_for(&"elixir_do_eco")
	var text := Alchemy.inputs_text(elixir)
	assert_true(text.contains("Cogumelo"), "usa o nome amigável: %s" % text)

func _recipe_for(output: StringName) -> Dictionary:
	for recipe in Alchemy.RECIPES:
		if recipe["output"] == output:
			return recipe
	return {}
