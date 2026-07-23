extends "res://addons/gut/test.gd"
## Testa os movesets-assinatura opcionais do DungeonBoss (§3.2): investida,
## salva de projéteis e invocação de capangas. Instancia sem adicionar à árvore
## para não disparar _ready (que depende de autoloads/save).

var scene: PackedScene

func before_all() -> void:
	scene = load("res://scenes/enemies/dungeon_boss.tscn")

func test_scene_loads() -> void:
	assert_not_null(scene, "dungeon_boss.tscn deve carregar")

func test_signature_moves_default_off() -> void:
	var boss = scene.instantiate()
	# Por padrão o chefe é um lutador corpo-a-corpo puro (sem assinaturas).
	assert_false(boss.lunge_on_attack, "investida desligada por padrão")
	assert_eq(boss.projectiles_per_attack, 0, "sem projéteis por padrão")
	assert_eq(boss.summon_count, 0, "sem invocação por padrão")
	assert_null(boss.summon_scene, "sem cena de invocação por padrão")
	boss.free()

func test_signature_methods_exist() -> void:
	var boss = scene.instantiate()
	assert_true(boss.has_method("_fire_volley"), "salva de projéteis")
	assert_true(boss.has_method("_summon"), "invocação de capangas")
	boss.free()

func test_projectile_scene_is_valid() -> void:
	# A pré-carga do projétil deve resolver (constante do script).
	var proj: PackedScene = load("res://scenes/combat/projectile.tscn")
	assert_not_null(proj)
	var p = proj.instantiate()
	assert_true(p.has_method("setup"), "projétil expõe setup()")
	p.free()

func test_boss_configs_have_signatures() -> void:
	# Cada dungeon liga a assinatura que faz sentido; validamos via texto do .tscn.
	var checks := {
		"res://scenes/world/floresta_sussurrante.tscn": "summon_count = 2",
		"res://scenes/world/forja_afundada.tscn": "lunge_on_attack = true",
		"res://scenes/world/torre_dos_ventos.tscn": "projectiles_per_attack = 1",
		"res://scenes/world/necropole_de_sal.tscn": "projectiles_per_attack = 3",
	}
	for path in checks:
		var f := FileAccess.open(path, FileAccess.READ)
		assert_not_null(f, "abre %s" % path)
		var text := f.get_as_text()
		assert_true(text.contains(checks[path]),
			"%s deve conter '%s'" % [path, checks[path]])
