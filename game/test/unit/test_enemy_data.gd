extends "res://addons/gut/test.gd"
## Testes de integridade dos dados de inimigo (§3.5/§6.3).

var comum: EnemyData
var noturno: EnemyData
var couracado: EnemyData
var arqueiro: EnemyData

func before_all() -> void:
	comum = load("res://data/enemies/ecoado_comum.tres")
	noturno = load("res://data/enemies/ecoado_noturno.tres")
	couracado = load("res://data/enemies/ecoado_couracado.tres")
	arqueiro = load("res://data/enemies/ecoado_arqueiro.tres")

func test_load() -> void:
	assert_not_null(comum)
	assert_not_null(noturno)
	assert_not_null(arqueiro, "ecoado_arqueiro.tres deve carregar")

func test_arqueiro_is_fragile_ranged() -> void:
	assert_eq(arqueiro.id, &"ecoado_arqueiro")
	# Frágil de perto: menos vida que o couraçado.
	assert_lt(arqueiro.max_health, couracado.max_health)

func test_alado_loads_and_is_fast_fragile() -> void:
	var alado: EnemyData = load("res://data/enemies/ecoado_alado.tres")
	assert_not_null(alado)
	assert_eq(alado.id, &"ecoado_alado")
	assert_lt(alado.max_health, couracado.max_health, "voador é frágil")
	assert_gt(alado.move_speed, couracado.move_speed, "voador é rápido")

func test_comum_is_diurnal() -> void:
	assert_false(comum.nocturnal, "Ecoado comum aparece de dia")

func test_noturno_is_nocturnal() -> void:
	assert_true(noturno.nocturnal, "Ecoado noturno só à noite")

func test_noturno_is_faster() -> void:
	assert_gt(noturno.move_speed, comum.move_speed)

func test_couracado_is_tanky() -> void:
	assert_not_null(couracado)
	assert_gt(couracado.max_health, comum.max_health, "mais vida")
	assert_gt(couracado.max_poise, comum.max_poise, "mais difícil de atordoar")

func test_couracado_is_slow() -> void:
	assert_lt(couracado.move_speed, comum.move_speed)

func test_positive_stats() -> void:
	for e in [comum, noturno, couracado]:
		assert_gt(e.max_health, 0.0)
		assert_gt(e.ecos_reward, 0)
