extends "res://addons/gut/test.gd"
## Testa as preferências de acessibilidade (§3.6): tremor de câmera e fonte.

func test_defaults_when_unset() -> void:
	# Sem chaves: tudo desligado (comportamento padrão do jogo).
	assert_false(Accessibility.is_reduce_shake({}))
	assert_false(Accessibility.is_large_text({}))
	assert_eq(Accessibility.shake_mult({}), 1.0, "tremor cheio por padrão")
	assert_eq(Accessibility.font_size({}), Accessibility.BASE_FONT_SIZE)

func test_reduce_shake_zeroes_multiplier() -> void:
	assert_true(Accessibility.is_reduce_shake({"reduce_shake": true}))
	assert_eq(Accessibility.shake_mult({"reduce_shake": true}), 0.0)

func test_large_text_bumps_font() -> void:
	assert_true(Accessibility.is_large_text({"large_text": true}))
	assert_eq(Accessibility.font_size({"large_text": true}), Accessibility.LARGE_FONT_SIZE)
	assert_gt(Accessibility.LARGE_FONT_SIZE, Accessibility.BASE_FONT_SIZE)

func test_apply_to_tree_sets_root_theme_font() -> void:
	var tree := get_tree()
	var previous: Theme = tree.root.theme
	Accessibility.apply_to_tree(tree, {"large_text": true})
	assert_not_null(tree.root.theme, "instala um tema na raiz")
	assert_eq(tree.root.theme.default_font_size, Accessibility.LARGE_FONT_SIZE)
	# Volta ao normal para não afetar outros testes.
	Accessibility.apply_to_tree(tree, {"large_text": false})
	assert_eq(tree.root.theme.default_font_size, Accessibility.BASE_FONT_SIZE)
	tree.root.theme = previous

func test_apply_to_tree_null_safe() -> void:
	# Não deve estourar com árvore nula.
	Accessibility.apply_to_tree(null, {})
	assert_true(true, "sem crash")
