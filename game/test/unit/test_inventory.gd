extends "res://addons/gut/test.gd"
## Testes das coleções de itens-chave/memórias (§3.3/§6.3) — gating metroidvania.

func test_grant_new_returns_true() -> void:
	var items: Array = []
	assert_true(Inventory.grant(&"gancho", items))
	assert_true(Inventory.has(&"gancho", items))

func test_grant_duplicate_returns_false() -> void:
	var items: Array = ["gancho"]
	assert_false(Inventory.grant(&"gancho", items))
	assert_eq(items.size(), 1)

func test_has_is_false_when_absent() -> void:
	assert_false(Inventory.has(&"gancho", []))

func test_grant_empty_ignored() -> void:
	var items: Array = []
	assert_false(Inventory.grant(&"", items))
	assert_eq(items.size(), 0)

func test_count() -> void:
	assert_eq(Inventory.count(["gancho", "memoria_01"]), 2)
