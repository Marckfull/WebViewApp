extends "res://addons/gut/test.gd"
## Testes dos nomes amigáveis de item (§3.5) usados pelo inventário.

func test_known_ids_have_labels() -> void:
	assert_eq(ItemNames.label(&"pocao_cura"), "Poção de Cura")
	assert_eq(ItemNames.label(&"minerio_do_eco"), "Minério do eco")
	assert_eq(ItemNames.label(&"botas"), "Botas de Corrente")

func test_unknown_id_falls_back_capitalized() -> void:
	# Sem entrada: capitaliza o id (nunca vazio).
	var label := ItemNames.label(&"algo_desconhecido")
	assert_ne(label, "")
	assert_eq(label, "Algo Desconhecido")

func test_key_items_all_named() -> void:
	for id in [&"gancho", &"bomba", &"lente", &"botas"]:
		assert_ne(ItemNames.label(id), String(id), "%s deve ter nome amigável" % id)

func test_forage_and_craft_items_named() -> void:
	assert_eq(ItemNames.label(&"madeira"), "Madeira")
	assert_eq(ItemNames.label(&"cogumelo"), "Cogumelo")
	assert_eq(ItemNames.label(&"peixe"), "Peixe")
	assert_eq(ItemNames.label(&"inseto"), "Inseto")
	assert_eq(ItemNames.label(&"erva_prateada"), "Erva prateada")
	assert_eq(ItemNames.label(&"erva_do_eco"), "Erva do eco")
	assert_eq(ItemNames.label(&"elixir_do_eco"), "Elixir do Eco")
	assert_eq(ItemNames.label(&"refeicao"), "Refeição Farta")
