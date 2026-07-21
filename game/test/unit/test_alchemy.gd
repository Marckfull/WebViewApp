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
