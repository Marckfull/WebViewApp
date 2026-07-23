extends "res://addons/gut/test.gd"
## Testa a sala intermediária da Necrópole (densidade §5): o átrio agora leva às
## Tumbas de Sal, e as Tumbas ligam às Salinas — ida e volta encadeadas.

func test_tumbas_scene_loads() -> void:
	var scene: PackedScene = load("res://scenes/world/necropole_tumbas.tscn")
	assert_not_null(scene, "necropole_tumbas.tscn deve carregar")

func test_atrio_leads_to_tumbas() -> void:
	assert_true(_text("res://scenes/world/necropole_atrio.tscn")
		.contains("necropole_tumbas.tscn"), "átrio deve levar às tumbas")

func test_tumbas_connects_both_ways() -> void:
	var t := _text("res://scenes/world/necropole_tumbas.tscn")
	assert_true(t.contains("necropole_atrio.tscn"), "tumbas volta ao átrio")
	assert_true(t.contains("necropole_de_sal.tscn"), "tumbas segue às salinas")

func test_salinas_returns_to_tumbas() -> void:
	assert_true(_text("res://scenes/world/necropole_de_sal.tscn")
		.contains("necropole_tumbas.tscn"), "salinas recua às tumbas")

func _text(path: String) -> String:
	var f := FileAccess.open(path, FileAccess.READ)
	assert_not_null(f, "abre %s" % path)
	return f.get_as_text()
