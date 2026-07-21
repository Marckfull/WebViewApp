extends "res://addons/gut/test.gd"
## Testes da forja / upgrade de arma (§3.3/§6.3).

func test_cost_scales_with_level() -> void:
	assert_gt(WeaponUpgrade.ecos_cost(2), WeaponUpgrade.ecos_cost(0))
	assert_gt(WeaponUpgrade.minerio_cost(3), WeaponUpgrade.minerio_cost(0))

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
