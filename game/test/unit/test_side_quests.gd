extends "res://addons/gut/test.gd"
## Integridade das side quests do documento de design (§3.5). Cada uma carrega,
## tem objetivo/alvo coerentes e está registrada no QuestManager.

const REGISTERED := [
	"res://data/quests/cao_sem_nome.tres",
	"res://data/quests/ervas_alquimista.tres",
	"res://data/quests/corvo_favor.tres",
	"res://data/quests/torre_vigia.tres",
	"res://data/quests/botas_primeira_ecoada.tres",
	"res://data/quests/duelo_arena.tres",
	"res://data/quests/coro_memorias.tres",
]

func test_quests_load_with_valid_fields() -> void:
	for path in REGISTERED:
		var q: QuestData = load(path)
		assert_not_null(q, "carrega %s" % path)
		assert_ne(String(q.id), "", "%s tem id" % path)
		assert_ne(q.title, "", "%s tem título" % path)
		assert_gt(q.count, 0, "%s exige ao menos 1" % path)
		assert_gt(q.reward_ecos, 0, "%s recompensa Ecos" % path)

func test_cao_sem_nome_is_a_kill_quest() -> void:
	var q: QuestData = load("res://data/quests/cao_sem_nome.tres")
	assert_eq(q.objective, QuestData.Objective.KILL)
	assert_eq(q.target, &"ecoado_comum")
	assert_eq(q.count, 2)

func test_ervas_alquimista_is_a_collect_quest() -> void:
	var q: QuestData = load("res://data/quests/ervas_alquimista.tres")
	assert_eq(q.objective, QuestData.Objective.COLLECT)
	assert_eq(q.target, &"erva")
	assert_eq(q.count, 5)

func test_duelo_arena_targets_the_ocaso_boss() -> void:
	var q: QuestData = load("res://data/quests/duelo_arena.tres")
	assert_eq(q.objective, QuestData.Objective.KILL)
	assert_eq(q.target, &"eco_maior_ocaso")

func test_coro_memorias_is_a_memories_quest() -> void:
	var q: QuestData = load("res://data/quests/coro_memorias.tres")
	assert_eq(q.objective, QuestData.Objective.MEMORIES)
	assert_eq(q.count, 12, "as 12 Memórias Perdidas")

func test_memories_objective_completes_by_collected_count() -> void:
	var q := QuestData.new()
	q.id = &"coro_memorias"
	q.objective = QuestData.Objective.MEMORIES
	q.count = 12
	var quests := {"coro_memorias": {"status": Quest.ACTIVE, "progress": 0}}
	assert_false(Quest.is_memories_ready(q, quests, 11), "11 não bastam")
	assert_true(Quest.is_memories_ready(q, quests, 12), "12 concluem")
	# Fora do estado ACTIVE nunca fica pronta.
	assert_false(Quest.is_memories_ready(q, {}, 12), "não iniciada não conclui")

func test_all_registered_in_quest_manager() -> void:
	for path in REGISTERED:
		assert_true(QuestManager.QUEST_PATHS.has(path),
			"%s deve estar em QuestManager.QUEST_PATHS" % path)
