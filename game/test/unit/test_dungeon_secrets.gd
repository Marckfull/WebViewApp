extends "res://addons/gut/test.gd"
## Testa os segredos das salas intermediárias (§5): cada uma esconde um nicho
## selado por parede quebrável (bomba) com um recurso raro dentro.

const SECRETS := {
	"res://scenes/world/floresta_bosque.tscn": ["bosque_secreto", "erva_do_eco"],
	"res://scenes/world/forja_fornalhas.tscn": ["fornalhas_secreto", "minerio_do_eco"],
	"res://scenes/world/torre_ventos_altos.tscn": ["ventos_secreto", "erva_prateada"],
	"res://scenes/world/necropole_tumbas.tscn": ["tumbas_secreto", "peixe"],
}

func test_each_room_loads() -> void:
	for path in SECRETS:
		assert_not_null(load(path), "carrega %s" % path)

func test_each_room_has_sealed_secret_cache() -> void:
	for path in SECRETS:
		var text := _text(path)
		var wall_id: String = SECRETS[path][0]
		var bonus: String = SECRETS[path][1]
		assert_true(text.contains("SecretWall"), "%s tem parede secreta" % path)
		assert_true(text.contains(wall_id), "%s usa wall_id %s" % [path, wall_id])
		assert_true(text.contains('required_item = &"bomba"'), "%s é selado por bomba" % path)
		assert_true(text.contains(bonus), "%s esconde %s" % [path, bonus])

func _text(path: String) -> String:
	var f := FileAccess.open(path, FileAccess.READ)
	assert_not_null(f, "abre %s" % path)
	return f.get_as_text()
