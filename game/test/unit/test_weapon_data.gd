extends "res://addons/gut/test.gd"
## Testes de integridade dos dados de arma (§3.3, §6.3). Garante que os .tres
## carregam e que o contraste de moveset (espada vs. adagas) é o pretendido.

var espada: WeaponData
var adaga: WeaponData

func before_all() -> void:
	espada = load("res://data/weapons/espada_guardia.tres")
	adaga = load("res://data/weapons/adaga_dupla.tres")

func test_weapons_load() -> void:
	assert_not_null(espada, "espada_guardia.tres deve carregar")
	assert_not_null(adaga, "adaga_dupla.tres deve carregar")

func test_ids_are_set() -> void:
	assert_eq(espada.id, &"espada_guardia")
	assert_eq(adaga.id, &"adaga_dupla")

func test_adaga_is_faster_but_weaker() -> void:
	assert_gt(adaga.attack_speed, espada.attack_speed, "adaga ataca mais rápido")
	assert_lt(adaga.base_damage, espada.base_damage, "adaga bate mais fraco")

func test_adaga_costs_less_stamina() -> void:
	assert_lt(adaga.stamina_cost, espada.stamina_cost)

func test_positive_values() -> void:
	for w in [espada, adaga]:
		assert_gt(w.base_damage, 0.0)
		assert_gt(w.stamina_cost, 0.0)
		assert_gt(w.attack_speed, 0.0)
