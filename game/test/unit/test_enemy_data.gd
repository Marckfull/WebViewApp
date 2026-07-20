extends "res://addons/gut/test.gd"
## Testes de integridade dos dados de inimigo (§3.5/§6.3).

var comum: EnemyData
var noturno: EnemyData

func before_all() -> void:
	comum = load("res://data/enemies/ecoado_comum.tres")
	noturno = load("res://data/enemies/ecoado_noturno.tres")

func test_load() -> void:
	assert_not_null(comum)
	assert_not_null(noturno)

func test_comum_is_diurnal() -> void:
	assert_false(comum.nocturnal, "Ecoado comum aparece de dia")

func test_noturno_is_nocturnal() -> void:
	assert_true(noturno.nocturnal, "Ecoado noturno só à noite")

func test_noturno_is_faster() -> void:
	assert_gt(noturno.move_speed, comum.move_speed)

func test_positive_stats() -> void:
	for e in [comum, noturno]:
		assert_gt(e.max_health, 0.0)
		assert_gt(e.ecos_reward, 0)
