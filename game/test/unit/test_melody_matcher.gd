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

func _crepusculo() -> MelodyData:
	var m := MelodyData.new()
	m.id = &"cancao_do_crepusculo"
	m.notes = [4, 3, 2, 1, 0]
	return m

func test_disambiguates_between_two_melodies() -> void:
	var lib := [_cancao, _crepusculo()]
	assert_eq(MelodyMatcher.exact_match([0, 2, 4, 2, 0], lib).id, &"cancao_do_mundo")
	assert_eq(MelodyMatcher.exact_match([4, 3, 2, 1, 0], lib).id, &"cancao_do_crepusculo")

func test_prefix_matches_correct_melody() -> void:
	var lib := [_cancao, _crepusculo()]
	# [4] só é prefixo do crepúsculo; [0] só do mundo — ambos ainda possíveis.
	assert_true(MelodyMatcher.any_prefix([4], lib))
	assert_true(MelodyMatcher.any_prefix([0], lib))
	# [4,2] não é prefixo de nenhuma (crepúsculo é 4,3,...).
	assert_false(MelodyMatcher.any_prefix([4, 2], lib))
