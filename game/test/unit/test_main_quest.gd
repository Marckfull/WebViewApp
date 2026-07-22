extends "res://addons/gut/test.gd"
## Testes da campanha do Ato 2 (§3.5): dados das 4 missões e a lógica pura de
## avanço de etapa (MainQuest).

const PATHS := [
	"res://data/quests/main/mp_floresta.tres",
	"res://data/quests/main/mp_forja.tres",
	"res://data/quests/main/mp_torre.tres",
	"res://data/quests/main/mp_necropole.tres",
]

func test_four_main_quests_load() -> void:
	for p in PATHS:
		var q: MainQuestData = load(p)
		assert_not_null(q, "deve carregar %s" % p)
		assert_gt(q.step_count(), 0)
		# arrays paralelos consistentes
		assert_eq(q.step_descs.size(), q.step_kinds.size())
		assert_eq(q.step_descs.size(), q.step_targets.size())

func test_step_matches_by_kind_and_target() -> void:
	var q: MainQuestData = load("res://data/quests/main/mp_floresta.tres")
	# etapa 0 = obter a lança
	assert_true(MainQuest.step_matches(q, 0, "obtain", &"lanca"))
	assert_false(MainQuest.step_matches(q, 0, "kill_boss", &"lanca"), "kind errado")
	assert_false(MainQuest.step_matches(q, 0, "obtain", &"martelo"), "alvo errado")
	# etapa 1 = derrotar o Coro Enraizado
	assert_true(MainQuest.step_matches(q, 1, "kill_boss", &"coro_enraizado"))

func test_finished_after_last_step() -> void:
	var q: MainQuestData = load("res://data/quests/main/mp_floresta.tres")
	assert_false(MainQuest.is_finished(q, 0))
	assert_false(MainQuest.is_finished(q, q.step_count() - 1))
	assert_true(MainQuest.is_finished(q, q.step_count()))

func test_out_of_range_step_never_matches() -> void:
	var q: MainQuestData = load("res://data/quests/main/mp_necropole.tres")
	assert_false(MainQuest.step_matches(q, 99, "obtain", &"botas"))
	assert_false(MainQuest.step_matches(null, 0, "obtain", &"botas"))
