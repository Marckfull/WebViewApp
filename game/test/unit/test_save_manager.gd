extends "res://addons/gut/test.gd"
## Testes do save JSON versionado com escrita atômica (§6.2, §6.3).

const SLOT := 9  # slot dedicado a testes, fora dos slots de jogo (0..3)

func after_each() -> void:
	var path := "user://saves/slot_%d.json" % SLOT
	if FileAccess.file_exists(path):
		DirAccess.remove_absolute(ProjectSettings.globalize_path(path))

func test_save_and_load_roundtrip() -> void:
	SaveManager.state["aria"]["ecos"] = 123
	assert_true(SaveManager.save_game(SLOT), "save deve ter sucesso")
	SaveManager.state["aria"]["ecos"] = 0
	assert_true(SaveManager.load_game(SLOT), "load deve ter sucesso")
	assert_eq(int(SaveManager.state["aria"]["ecos"]), 123)

func test_has_save_true_after_save() -> void:
	SaveManager.save_game(SLOT)
	assert_true(SaveManager.has_save(SLOT))

func test_load_missing_slot_returns_false() -> void:
	assert_false(SaveManager.load_game(7))

func test_saved_version_is_current() -> void:
	SaveManager.save_game(SLOT)
	SaveManager.load_game(SLOT)
	assert_eq(int(SaveManager.state["version"]), SaveManager.SAVE_VERSION)
