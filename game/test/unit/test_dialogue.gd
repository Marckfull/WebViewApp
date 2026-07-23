extends "res://addons/gut/test.gd"
## Testes do diálogo ramificado (§3.5): dados carregam e a árvore de escolhas
## aponta para os ramos certos (ou encerra em null).

func test_linear_dialogue_has_no_choices() -> void:
	var d: DialogueData = load("res://data/dialogue/corvo_intro.tres")
	assert_not_null(d)
	assert_false(d.has_choices(), "corvo_intro é linear")

func test_branching_root_has_choices() -> void:
	var d: DialogueData = load("res://data/dialogue/errante_lore.tres")
	assert_not_null(d)
	assert_true(d.has_choices())
	assert_eq(d.choice_texts.size(), 3)

func test_choices_point_to_branches() -> void:
	var d: DialogueData = load("res://data/dialogue/errante_lore.tres")
	var g := d.next_for(0)
	var s := d.next_for(1)
	assert_not_null(g)
	assert_not_null(s)
	assert_eq(g.id, &"errante_guardias")
	assert_eq(s.id, &"errante_silencio")
	# A 3ª escolha encerra a conversa.
	assert_null(d.next_for(2))

func test_next_for_out_of_range_is_null() -> void:
	var d: DialogueData = load("res://data/dialogue/errante_lore.tres")
	assert_null(d.next_for(99))
	assert_null(d.next_for(-1))
