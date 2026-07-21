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

func test_offer_count_uses_shortest_array() -> void:
	var s := ShopData.new()
	s.item_ids = [&"a", &"b"] as Array[StringName]
	s.labels = ["A"] as Array[String]
	s.prices = [1, 2] as Array[int]
	s.bags = ["consumables", "resources"] as Array[String]
	assert_eq(s.offer_count(), 1)
