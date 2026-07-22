extends "res://addons/gut/test.gd"
## Testes da seleção de slots de consumível (§3.5/§6.3).

func test_next_slot_skips_empty() -> void:
	var slots := [&"a", &"", &"c", &""]
	assert_eq(ConsumableSlots.next_slot(slots, 0), 2)  # pula o vazio 1
	assert_eq(ConsumableSlots.next_slot(slots, 2), 0)  # dá a volta, pula o 3

func test_next_slot_all_empty_keeps_current() -> void:
	assert_eq(ConsumableSlots.next_slot([&"", &""], 0), 0)

func test_default_cycles_through_four_filled() -> void:
	# Os 4 slots trazem poções + craftáveis; o ciclo passa por todos e dá a volta.
	assert_eq(ConsumableSlots.next_slot(ConsumableSlots.DEFAULT, 0), 1)
	assert_eq(ConsumableSlots.next_slot(ConsumableSlots.DEFAULT, 1), 2)
	assert_eq(ConsumableSlots.next_slot(ConsumableSlots.DEFAULT, 2), 3)
	assert_eq(ConsumableSlots.next_slot(ConsumableSlots.DEFAULT, 3), 0)

func test_short_name() -> void:
	assert_eq(ConsumableSlots.short_name(&"pocao_vigor"), "Vigor")
	assert_eq(ConsumableSlots.short_name(&"pocao_cura"), "Cura")
	assert_eq(ConsumableSlots.short_name(&"elixir_do_eco"), "Elixir")
	assert_eq(ConsumableSlots.short_name(&"refeicao"), "Refeição")
	assert_eq(ConsumableSlots.short_name(&"desconhecido"), "—")
