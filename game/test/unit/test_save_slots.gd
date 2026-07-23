extends "res://addons/gut/test.gd"
## Testes dos slots manuais de save (§3.6): peek e isolamento de estado.

const SLOT := 3

func after_each() -> void:
	var path := "user://saves/slot_%d.json" % SLOT
	if FileAccess.file_exists(path):
		DirAccess.remove_absolute(ProjectSettings.globalize_path(path))

func test_peek_empty_slot_returns_empty() -> void:
	assert_true(SaveManager.peek(8).is_empty())

func test_save_then_peek_reads_summary() -> void:
	SaveManager.state["aria"]["ecos"] = 77
	SaveManager.save_game(SLOT)
	var d := SaveManager.peek(SLOT)
	assert_false(d.is_empty())
	assert_eq(int(d["aria"]["ecos"]), 77)

func test_peek_does_not_change_current_state() -> void:
	SaveManager.save_game(SLOT)
	SaveManager.state["aria"]["ecos"] = 999
	SaveManager.peek(SLOT)  # só lê o arquivo; não mexe no estado em memória
	assert_eq(int(SaveManager.state["aria"]["ecos"]), 999)
