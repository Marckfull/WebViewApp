extends "res://addons/gut/test.gd"
## Testes do estado de side quests (§3.5/§6.3).

var q: QuestData

func before_each() -> void:
	q = QuestData.new()
	q.id = &"matar_couracado"
	q.count = 2

func test_unknown_quest_is_unstarted() -> void:
	assert_eq(Quest.status({}, &"matar_couracado"), Quest.UNSTARTED)
	assert_eq(Quest.progress({}, &"matar_couracado"), 0)

func test_reads_status_and_progress() -> void:
	var quests := {"matar_couracado": {"status": Quest.ACTIVE, "progress": 1}}
	assert_eq(Quest.status(quests, &"matar_couracado"), Quest.ACTIVE)
	assert_eq(Quest.progress(quests, &"matar_couracado"), 1)

func test_not_ready_when_progress_below_count() -> void:
	var quests := {"matar_couracado": {"status": Quest.ACTIVE, "progress": 1}}
	assert_false(Quest.is_ready_to_complete(q, quests))

func test_ready_when_progress_meets_count() -> void:
	var quests := {"matar_couracado": {"status": Quest.ACTIVE, "progress": 2}}
	assert_true(Quest.is_ready_to_complete(q, quests))

func test_not_ready_when_unstarted() -> void:
	assert_false(Quest.is_ready_to_complete(q, {}))

func test_all_quest_data_loads() -> void:
	for path in ["res://data/quests/matar_couracado.tres",
			"res://data/quests/silenciar_arqueiros.tres",
			"res://data/quests/cacar_noturno.tres"]:
		var qd: QuestData = load(path)
		assert_not_null(qd, "deve carregar %s" % path)
		assert_ne(String(qd.target), "", "alvo definido em %s" % path)
		assert_gt(qd.count, 0)
		assert_gt(qd.reward_ecos, 0)

func test_arqueiros_quest_targets_two() -> void:
	var qd: QuestData = load("res://data/quests/silenciar_arqueiros.tres")
	assert_eq(qd.target, &"ecoado_arqueiro")
	assert_eq(qd.count, 2)
