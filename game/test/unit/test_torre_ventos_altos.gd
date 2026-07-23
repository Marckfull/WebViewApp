extends "res://addons/gut/test.gd"
## Testa a sala intermediária da Torre (densidade §5): o átrio agora leva aos
## Ventos Altos, e os Ventos Altos ligam à Cúpula — ida e volta encadeadas.

func test_ventos_scene_loads() -> void:
	var scene: PackedScene = load("res://scenes/world/torre_ventos_altos.tscn")
	assert_not_null(scene, "torre_ventos_altos.tscn deve carregar")

func test_atrio_leads_to_ventos() -> void:
	assert_true(_text("res://scenes/world/torre_atrio.tscn")
		.contains("torre_ventos_altos.tscn"), "átrio deve levar aos ventos altos")

func test_ventos_connects_both_ways() -> void:
	var t := _text("res://scenes/world/torre_ventos_altos.tscn")
	assert_true(t.contains("torre_atrio.tscn"), "ventos altos volta ao átrio")
	assert_true(t.contains("torre_dos_ventos.tscn"), "ventos altos segue à cúpula")

func test_cupula_returns_to_ventos() -> void:
	assert_true(_text("res://scenes/world/torre_dos_ventos.tscn")
		.contains("torre_ventos_altos.tscn"), "cúpula recua aos ventos altos")

func _text(path: String) -> String:
	var f := FileAccess.open(path, FileAccess.READ)
	assert_not_null(f, "abre %s" % path)
	return f.get_as_text()
