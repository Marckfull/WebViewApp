extends "res://addons/gut/test.gd"
## Testes da progressão de atributos (§3.3, §6.3).

func _fresh_attrs() -> Dictionary:
	return { "vitalidade": 1, "stamina": 1, "forca": 1, "destreza": 1, "harmonia": 1 }

func test_total_level_starts_at_five() -> void:
	assert_eq(Attributes.total_level(_fresh_attrs()), 5)

func test_cost_scales_with_total_level() -> void:
	var low := Attributes.cost_for_total_level(5)
	var high := Attributes.cost_for_total_level(10)
	assert_gt(high, low)

func test_max_hp_grows_with_vitalidade() -> void:
	assert_almost_eq(Attributes.max_hp_for(1), Attributes.BASE_HP, 0.01)
	assert_gt(Attributes.max_hp_for(2), Attributes.max_hp_for(1))

func test_max_stamina_grows_with_stamina() -> void:
	assert_gt(Attributes.max_stamina_for(3), Attributes.max_stamina_for(1))

func test_level_up_succeeds_with_enough_ecos() -> void:
	var attrs := _fresh_attrs()
	var cost := Attributes.cost_for_total_level(Attributes.total_level(attrs))
	var r := Attributes.try_level_up("vitalidade", attrs, cost + 10)
	assert_true(r["ok"])
	assert_eq(int(r["attrs"]["vitalidade"]), 2)
	assert_eq(int(r["ecos"]), 10)

func test_level_up_fails_without_ecos() -> void:
	var attrs := _fresh_attrs()
	var r := Attributes.try_level_up("vitalidade", attrs, 0)
	assert_false(r["ok"])
	assert_eq(int(r["attrs"]["vitalidade"]), 1)

func test_level_up_rejects_unknown_attribute() -> void:
	var r := Attributes.try_level_up("inexistente", _fresh_attrs(), 9999)
	assert_false(r["ok"])

func test_try_level_up_does_not_mutate_input() -> void:
	var attrs := _fresh_attrs()
	Attributes.try_level_up("vitalidade", attrs, 9999)
	assert_eq(int(attrs["vitalidade"]), 1, "o dicionário original não deve mudar")
