extends "res://addons/gut/test.gd"
## Testa as cores de ícone greybox e a montagem de células da grade (§3.5).

func test_known_items_have_curated_colors() -> void:
	# Itens conhecidos usam a cor curada exata da tabela.
	assert_eq(ItemIcons.color_for(&"pocao_cura"), Color(0.85, 0.3, 0.3))
	assert_eq(ItemIcons.color_for(&"madeira"), Color(0.55, 0.4, 0.25))

func test_unknown_color_is_deterministic_and_bright() -> void:
	var a := ItemIcons.color_for(&"item_misterioso")
	var b := ItemIcons.color_for(&"item_misterioso")
	assert_eq(a, b, "mesma id -> mesma cor (determinístico)")
	# Nunca escura demais: cada canal parte de 0.35.
	assert_gte(a.r, 0.35)
	assert_gte(a.g, 0.35)
	assert_gte(a.b, 0.35)

func test_short_label_takes_first_word() -> void:
	assert_eq(ItemIcons.short_label(&"elixir_do_eco"), "Elixir")
	assert_eq(ItemIcons.short_label(&"madeira"), "Madeira")

func test_cells_from_skips_zero_and_keeps_counts() -> void:
	var cells := ItemIcons.cells_from({"madeira": 3, "peixe": 0, "cogumelo": 2})
	assert_eq(cells.size(), 2, "ignora quantidade zero")
	var ids := {}
	for c in cells:
		ids[String(c["id"])] = int(c["count"])
	assert_eq(ids.get("madeira"), 3)
	assert_eq(ids.get("cogumelo"), 2)
	assert_false(ids.has("peixe"), "peixe zerado não entra")

func test_cells_from_empty_bag() -> void:
	assert_eq(ItemIcons.cells_from({}).size(), 0)
