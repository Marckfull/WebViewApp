extends "res://addons/gut/test.gd"
## Testes do equipamento (§3.3): agregação de bônus, teto de redução e resolução
## de id. Lógica pura (Equipment) — sem UI/estado.

func test_pieces_load() -> void:
	var couraca := Equipment.by_id(&"couraca_de_sal")
	var amuleto := Equipment.by_id(&"amuleto_do_eco")
	assert_not_null(couraca)
	assert_not_null(amuleto)
	assert_eq(couraca.slot, EquipmentData.Slot.ARMOR)
	assert_eq(amuleto.slot, EquipmentData.Slot.AMULET)

func test_by_id_unknown_is_null() -> void:
	assert_null(Equipment.by_id(&"inexistente"))
	assert_null(Equipment.by_id(&""))

func test_aggregate_sums_bonuses() -> void:
	var couraca := Equipment.by_id(&"couraca_de_sal")
	var amuleto := Equipment.by_id(&"amuleto_do_eco")
	var agg := Equipment.aggregate([couraca, amuleto])
	assert_almost_eq(agg["max_hp_bonus"], couraca.max_hp_bonus + amuleto.max_hp_bonus, 0.01)
	assert_almost_eq(agg["stamina_regen_bonus"], amuleto.stamina_regen_bonus, 0.01)
	assert_gt(agg["damage_reduction"], 0.0)

func test_empty_aggregate_is_neutral() -> void:
	var agg := Equipment.aggregate([])
	assert_almost_eq(agg["damage_reduction"], 0.0, 0.001)
	assert_almost_eq(agg["max_hp_bonus"], 0.0, 0.001)
	assert_almost_eq(agg["stamina_regen_bonus"], 0.0, 0.001)

func test_reduction_capped() -> void:
	# Muitas peças não devem passar do teto (nunca invulnerável).
	var heavy := EquipmentData.new()
	heavy.damage_reduction = 0.8
	var agg := Equipment.aggregate([heavy, heavy, heavy])
	assert_lte(agg["damage_reduction"], Equipment.MAX_REDUCTION)

func test_null_pieces_ignored() -> void:
	var agg := Equipment.aggregate([null, null])
	assert_almost_eq(agg["max_hp_bonus"], 0.0, 0.001)
