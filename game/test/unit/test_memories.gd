extends "res://addons/gut/test.gd"
## Testes das 12 Memórias Perdidas (§2). Garante que todos os .tres carregam, têm
## id único e conteúdo — o final secreto depende de coletar as 12.

func test_all_twelve_memories_load_with_unique_ids() -> void:
	var ids: Array = []
	for i in range(1, 13):
		var path := "res://data/dialogue/memoria_%02d.tres" % i
		var d: DialogueData = load(path)
		assert_not_null(d, "faltou %s" % path)
		var expected := StringName("memoria_%02d" % i)
		assert_eq(d.id, expected)
		assert_gt(d.lines.size(), 0)
		assert_false(ids.has(d.id), "id repetido: %s" % d.id)
		ids.append(d.id)
	assert_eq(ids.size(), 12)
