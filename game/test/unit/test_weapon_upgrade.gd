extends "res://addons/gut/test.gd"
## Testes da forja / upgrade de arma (§3.3/§6.3).

func test_ecos_cost_scales_with_level() -> void:
	assert_gt(WeaponUpgrade.ecos_cost(2), WeaponUpgrade.ecos_cost(0))

func test_required_tier_escalates() -> void:
	# Níveis mais altos exigem minério mais raro.
	assert_eq(WeaponUpgrade.required_tier(0), WeaponUpgrade.TIER_BRUTO)
	assert_eq(WeaponUpgrade.required_tier(1), WeaponUpgrade.TIER_BRUTO)
	assert_eq(WeaponUpgrade.required_tier(2), WeaponUpgrade.TIER_RESSONANTE)
	assert_eq(WeaponUpgrade.required_tier(3), WeaponUpgrade.TIER_RESSONANTE)
	assert_eq(WeaponUpgrade.required_tier(4), WeaponUpgrade.TIER_ECO)
	# os três tiers são distintos
	assert_ne(WeaponUpgrade.TIER_BRUTO, WeaponUpgrade.TIER_RESSONANTE)
	assert_ne(WeaponUpgrade.TIER_RESSONANTE, WeaponUpgrade.TIER_ECO)

func test_tier_labels_present() -> void:
	for t in [WeaponUpgrade.TIER_BRUTO, WeaponUpgrade.TIER_RESSONANTE, WeaponUpgrade.TIER_ECO]:
		assert_ne(WeaponUpgrade.tier_label(t), "")

func test_damage_multiplier_grows() -> void:
	assert_almost_eq(WeaponUpgrade.damage_multiplier(0), 1.0, 0.001)
	assert_gt(WeaponUpgrade.damage_multiplier(3), WeaponUpgrade.damage_multiplier(1))

func test_can_upgrade_true_with_enough() -> void:
	var lvl := 0
	assert_true(WeaponUpgrade.can_upgrade(lvl, WeaponUpgrade.ecos_cost(lvl), WeaponUpgrade.minerio_cost(lvl)))

func test_cannot_upgrade_without_ecos() -> void:
	assert_false(WeaponUpgrade.can_upgrade(0, 0, 99))

func test_cannot_upgrade_without_minerio() -> void:
	assert_false(WeaponUpgrade.can_upgrade(0, 9999, 0))

func test_cannot_upgrade_past_max() -> void:
	assert_false(WeaponUpgrade.can_upgrade(WeaponUpgrade.MAX_LEVEL, 99999, 99))
