extends "res://addons/gut/test.gd"
## Testa a sala intermediária da Floresta (densidade §5): o átrio agora leva ao
## Bosque, e o Bosque liga à Sussurrante — ida e volta encadeadas.

func test_bosque_scene_loads() -> void:
	var scene: PackedScene = load("res://scenes/world/floresta_bosque.tscn")
	assert_not_null(scene, "floresta_bosque.tscn deve carregar")

func test_atrio_leads_to_bosque() -> void:
	assert_true(_text("res://scenes/world/floresta_atrio.tscn")
		.contains("floresta_bosque.tscn"), "átrio deve levar ao bosque")

func test_bosque_connects_both_ways() -> void:
	var t := _text("res://scenes/world/floresta_bosque.tscn")
	assert_true(t.contains("floresta_atrio.tscn"), "bosque volta ao átrio")
	assert_true(t.contains("floresta_sussurrante.tscn"), "bosque segue à sussurrante")

func test_sussurrante_returns_to_bosque() -> void:
	assert_true(_text("res://scenes/world/floresta_sussurrante.tscn")
		.contains("floresta_bosque.tscn"), "sussurrante recua ao bosque")

func _text(path: String) -> String:
	var f := FileAccess.open(path, FileAccess.READ)
	assert_not_null(f, "abre %s" % path)
	return f.get_as_text()
