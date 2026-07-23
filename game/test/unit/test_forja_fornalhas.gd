extends "res://addons/gut/test.gd"
## Testa a sala intermediária da Forja (densidade §5): o átrio agora leva às
## Fornalhas, e as Fornalhas ligam à Forja Afundada — ida e volta encadeadas.

func test_fornalhas_scene_loads() -> void:
	var scene: PackedScene = load("res://scenes/world/forja_fornalhas.tscn")
	assert_not_null(scene, "forja_fornalhas.tscn deve carregar")

func test_atrio_leads_to_fornalhas() -> void:
	assert_true(_text("res://scenes/world/forja_atrio.tscn")
		.contains("forja_fornalhas.tscn"), "átrio deve levar às fornalhas")

func test_fornalhas_connects_both_ways() -> void:
	var t := _text("res://scenes/world/forja_fornalhas.tscn")
	assert_true(t.contains("forja_atrio.tscn"), "fornalhas volta ao átrio")
	assert_true(t.contains("forja_afundada.tscn"), "fornalhas segue à forja afundada")

func test_afundada_returns_to_fornalhas() -> void:
	assert_true(_text("res://scenes/world/forja_afundada.tscn")
		.contains("forja_fornalhas.tscn"), "forja afundada recua às fornalhas")

func _text(path: String) -> String:
	var f := FileAccess.open(path, FileAccess.READ)
	assert_not_null(f, "abre %s" % path)
	return f.get_as_text()
