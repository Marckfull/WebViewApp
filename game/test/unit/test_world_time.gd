extends "res://addons/gut/test.gd"
## Testes do ciclo dia/noite (§3.5): o limiar que NPCs e inimigos usam.

func test_day_is_not_night() -> void:
	assert_true(WorldTime.is_day(0.0))
	assert_false(WorldTime.is_night(0.0))

func test_night_at_threshold() -> void:
	assert_true(WorldTime.is_night(WorldTime.NIGHT_THRESHOLD))
	assert_true(WorldTime.is_night(0.75))
	assert_false(WorldTime.is_day(0.5))

func test_day_and_night_are_exclusive() -> void:
	for t in [0.0, 0.25, 0.5, 0.9, 1.0]:
		assert_ne(WorldTime.is_day(t), WorldTime.is_night(t))
