extends "res://addons/gut/test.gd"
## Testes da loja do mercador (§3.5). Cobre a aritmética pura de compra e a
## integridade dos dados da loja do Corvo.

func test_can_afford() -> void:
	assert_true(Shop.can_afford(30, 30))
	assert_true(Shop.can_afford(30, 100))
	assert_false(Shop.can_afford(30, 29))

func test_try_buy_deducts_when_affordable() -> void:
	var res := Shop.try_buy(30, 100)
	assert_true(res["ok"])
	assert_eq(res["ecos"], 70)

func test_try_buy_refuses_when_broke() -> void:
	var res := Shop.try_buy(30, 10)
	assert_false(res["ok"])
	assert_eq(res["ecos"], 10)  # não gasta nada

func test_shop_data_loads_and_is_consistent() -> void:
	var s: ShopData = load("res://data/shops/corvo.tres")
	assert_not_null(s)
	assert_eq(s.shop_id, &"corvo")
	assert_gt(s.offer_count(), 0)
	# arrays paralelos alinhados
	assert_eq(s.item_ids.size(), s.labels.size())
	assert_eq(s.item_ids.size(), s.prices.size())
	assert_eq(s.item_ids.size(), s.bags.size())
	# bolsas válidas
	for b in s.bags:
		assert_true(b == "consumables" or b == "resources")

func test_sell_value_is_positive_and_known() -> void:
	# Itens conhecidos têm valor de revenda tabelado.
	assert_eq(Shop.sell_value(&"pocao_cura"), 15)
	assert_eq(Shop.sell_value(&"minerio_do_eco"), 35)
	# Desconhecido cai num valor mínimo positivo.
	assert_gt(Shop.sell_value(&"algo_qualquer"), 0)

func test_forage_resources_are_sellable() -> void:
	# Os novos recursos de forrageio têm valor de revenda tabelado e positivo.
	for id in [&"madeira", &"cogumelo", &"peixe", &"inseto", &"erva_prateada", &"erva_do_eco"]:
		assert_gt(Shop.sell_value(id), 0, "%s deve ter valor de revenda" % id)
	# Tiers de erva valem mais conforme a potência.
	assert_gt(Shop.sell_value(&"erva_prateada"), Shop.sell_value(&"erva"))
	assert_gt(Shop.sell_value(&"erva_do_eco"), Shop.sell_value(&"erva_prateada"))

func test_sell_value_below_buy_price() -> void:
	# Vender uma poção de cura rende menos que comprá-la (loja do Corvo: 40).
	var s: ShopData = load("res://data/shops/corvo.tres")
	var buy := 0
	for i in s.offer_count():
		if s.item_ids[i] == &"pocao_cura":
			buy = s.prices[i]
	assert_gt(buy, 0)
	assert_lt(Shop.sell_value(&"pocao_cura"), buy)

func test_offer_count_uses_shortest_array() -> void:
	var s := ShopData.new()
	s.item_ids = [&"a", &"b"] as Array[StringName]
	s.labels = ["A"] as Array[String]
	s.prices = [1, 2] as Array[int]
	s.bags = ["consumables", "resources"] as Array[String]
	assert_eq(s.offer_count(), 1)
