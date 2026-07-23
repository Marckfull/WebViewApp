extends "res://addons/gut/test.gd"
## Testes dos consumíveis contáveis (§3.5/§6.3).

func test_grant_adds_count() -> void:
	var bag: Dictionary = {}
	Consumables.grant(&"pocao_vigor", bag, 2)
	assert_eq(Consumables.count(&"pocao_vigor", bag), 2)

func test_grant_accumulates() -> void:
	var bag: Dictionary = {"pocao_vigor": 1}
	Consumables.grant(&"pocao_vigor", bag)
	assert_eq(Consumables.count(&"pocao_vigor", bag), 2)

func test_consume_decrements_and_returns_true() -> void:
	var bag: Dictionary = {"pocao_vigor": 1}
	assert_true(Consumables.consume(&"pocao_vigor", bag))
	assert_eq(Consumables.count(&"pocao_vigor", bag), 0)

func test_consume_empty_returns_false() -> void:
	var bag: Dictionary = {}
	assert_false(Consumables.consume(&"pocao_vigor", bag))

func test_grant_zero_or_negative_ignored() -> void:
	var bag: Dictionary = {}
	Consumables.grant(&"pocao_vigor", bag, 0)
	Consumables.grant(&"pocao_vigor", bag, -3)
	assert_eq(Consumables.count(&"pocao_vigor", bag), 0)
