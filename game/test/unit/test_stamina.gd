extends "res://addons/gut/test.gd"
## Testes da economia de stamina — o coração do combate souls-like (§3.2, §6.3).

var stamina: StaminaComponent

func before_each() -> void:
	stamina = StaminaComponent.new()
	stamina.max_stamina = 100.0
	stamina.regen_per_second = 50.0
	add_child_autofree(stamina)  # dispara _ready (aplica multiplicador de dificuldade)

func test_starts_full() -> void:
	assert_almost_eq(stamina.current, stamina.max_stamina, 0.01)

func test_spend_reduces_current() -> void:
	var before := stamina.current
	assert_true(stamina.try_spend(20.0))
	assert_almost_eq(stamina.current, before - 20.0, 0.01)

func test_cannot_overspend() -> void:
	stamina.current = 10.0
	assert_false(stamina.try_spend(20.0))
	assert_almost_eq(stamina.current, 10.0, 0.01)

func test_spend_exact_amount_succeeds() -> void:
	stamina.current = 20.0
	assert_true(stamina.try_spend(20.0))
	assert_almost_eq(stamina.current, 0.0, 0.01)

func test_has_at_least() -> void:
	stamina.current = 30.0
	assert_true(stamina.has_at_least(30.0))
	assert_false(stamina.has_at_least(30.1))
