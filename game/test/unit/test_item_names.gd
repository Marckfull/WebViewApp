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
