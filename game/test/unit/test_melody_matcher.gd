extends "res://addons/gut/test.gd"
## Testes do reconhecimento de melodias da Ocarina (§3.1, §6.3).

var _cancao: MelodyData

func before_each() -> void:
	_cancao = MelodyData.new()
	_cancao.id = &"cancao_do_mundo"
	_cancao.notes = [0, 2, 4, 2, 0]

func test_exact_match_returns_melody() -> void:
	assert_eq(MelodyMatcher.exact_match([0, 2, 4, 2, 0], [_cancao]), _cancao)

func test_wrong_sequence_no_match() -> void:
	assert_null(MelodyMatcher.exact_match([0, 1, 2], [_cancao]))

func test_partial_is_not_exact_match() -> void:
	assert_null(MelodyMatcher.exact_match([0, 2, 4], [_cancao]))

func test_prefix_true() -> void:
	assert_true(MelodyMatcher.any_prefix([0, 2], [_cancao]))

func test_prefix_false() -> void:
	assert_false(MelodyMatcher.any_prefix([1, 3], [_cancao]))

func test_full_is_prefix_of_itself() -> void:
	assert_true(MelodyMatcher.is_prefix([0, 2, 4, 2, 0], _cancao.notes))

func test_longer_than_full_not_prefix() -> void:
	assert_false(MelodyMatcher.is_prefix([0, 2, 4, 2, 0, 1], _cancao.notes))

func test_empty_is_prefix() -> void:
	assert_true(MelodyMatcher.is_prefix([], _cancao.notes))
