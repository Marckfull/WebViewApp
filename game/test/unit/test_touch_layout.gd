extends "res://addons/gut/test.gd"
## Testa o layout de toque customizável (§3.6): escala e lado dos botões.

func test_defaults() -> void:
	assert_false(TouchLayout.is_left_handed({}))
	assert_eq(TouchLayout.button_scale({}), 1.0)
	assert_eq(TouchLayout.button_size({}), TouchLayout.BASE_BUTTON)
	assert_eq(TouchLayout.button_preset({}), Control.PRESET_BOTTOM_RIGHT)

func test_scale_is_clamped() -> void:
	assert_eq(TouchLayout.button_scale({"touch_scale": 5.0}), TouchLayout.MAX_SCALE)
	assert_eq(TouchLayout.button_scale({"touch_scale": 0.1}), TouchLayout.MIN_SCALE)

func test_button_size_scales() -> void:
	var big := TouchLayout.button_size({"touch_scale": 1.6})
	assert_gt(big.x, TouchLayout.BASE_BUTTON.x)
	assert_eq(big, TouchLayout.BASE_BUTTON * 1.6)

func test_next_scale_cycles() -> void:
	var s := TouchLayout.MIN_SCALE
	var seen := [s]
	for i in 10:
		s = TouchLayout.next_scale(s)
		seen.append(s)
		if s == TouchLayout.MIN_SCALE:
			break
	# Deve subir acima do mínimo e, em algum momento, voltar ao mínimo.
	assert_true(seen.max() > TouchLayout.MIN_SCALE, "sobe a escala")
	assert_lte(seen.max(), TouchLayout.MAX_SCALE, "nunca passa do máximo")
	assert_eq(s, TouchLayout.MIN_SCALE, "cicla de volta ao mínimo")

func test_left_handed_flips_preset_and_mirrors_x() -> void:
	var lefty := {"touch_left_handed": true}
	assert_true(TouchLayout.is_left_handed(lefty))
	assert_eq(TouchLayout.button_preset(lefty), Control.PRESET_BOTTOM_LEFT)
	# Um offset ancorado à direita (x negativo) vira x positivo p/ o canto esquerdo.
	var mirrored := TouchLayout.offset_for(Vector2(-70, -70), lefty)
	assert_gt(mirrored.x, 0.0, "espelhado para a esquerda")
	assert_eq(mirrored.y, -70.0, "y não muda")

func test_offset_scales_when_right_handed() -> void:
	var scaled := TouchLayout.offset_for(Vector2(-100, -100), {"touch_scale": 1.5})
	assert_eq(scaled, Vector2(-150, -150), "escala mantém o sinal (destro)")
