extends "res://addons/gut/test.gd"
## Testes de integridade dos dados de arma (§3.3, §6.3). Garante que os .tres
## carregam e que o contraste de moveset (espada vs. adagas) é o pretendido.

var espada: WeaponData
var adaga: WeaponData
var lanca: WeaponData
var martelo: WeaponData

func before_all() -> void:
	espada = load("res://data/weapons/espada_guardia.tres")
	adaga = load("res://data/weapons/adaga_dupla.tres")
	lanca = load("res://data/weapons/lanca.tres")
	martelo = load("res://data/weapons/martelo.tres")

func test_weapons_load() -> void:
	assert_not_null(espada, "espada_guardia.tres deve carregar")
	assert_not_null(adaga, "adaga_dupla.tres deve carregar")
	assert_not_null(lanca, "lanca.tres deve carregar")
	assert_not_null(martelo, "martelo.tres deve carregar")

func test_ids_are_set() -> void:
	assert_eq(espada.id, &"espada_guardia")
	assert_eq(adaga.id, &"adaga_dupla")

func test_adaga_is_faster_but_weaker() -> void:
	assert_gt(adaga.attack_speed, espada.attack_speed, "adaga ataca mais rápido")
	assert_lt(adaga.base_damage, espada.base_damage, "adaga bate mais fraco")

func test_adaga_costs_less_stamina() -> void:
	assert_lt(adaga.stamina_cost, espada.stamina_cost)

func test_lanca_reaches_farther() -> void:
	# A lança bate de mais longe que as armas curtas.
	assert_gt(lanca.reach, espada.reach)
	assert_gt(lanca.reach, martelo.reach)

func test_arco_is_ranged_class() -> void:
	var arco: WeaponData = load("res://data/weapons/arco.tres")
	assert_not_null(arco)
	assert_eq(arco.id, &"arco")
	assert_eq(arco.weapon_class, WeaponData.WeaponClass.ARCO)

func test_chicote_longest_reach_low_poise() -> void:
	var chicote: WeaponData = load("res://data/weapons/chicote.tres")
	assert_not_null(chicote)
	assert_eq(chicote.weapon_class, WeaponData.WeaponClass.CHICOTE)
	# Alcança mais que a lança e mal abala a postura (arma de espaçamento).
	assert_gte(chicote.reach, lanca.reach)
	assert_lt(chicote.poise_damage, espada.poise_damage)

func test_martelo_is_heavy_hitter() -> void:
	# Martelo: mais dano e MUITO mais quebra de postura, porém lento e caro.
	assert_gt(martelo.base_damage, espada.base_damage)
	assert_gt(martelo.poise_damage, espada.poise_damage)
	assert_lt(martelo.attack_speed, espada.attack_speed)
	assert_gt(martelo.stamina_cost, espada.stamina_cost)

func test_positive_values() -> void:
	for w in [espada, adaga, lanca, martelo]:
		assert_gt(w.base_damage, 0.0)
		assert_gt(w.stamina_cost, 0.0)
		assert_gt(w.attack_speed, 0.0)
		assert_gt(w.reach, 0.0)
