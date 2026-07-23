extends "res://addons/gut/test.gd"
## Testes das regras de dificuldade Balada / Canção / Requiem (§3.4, §6.3).

func after_all() -> void:
	GameConfig.difficulty = GameConfig.Difficulty.CANCAO  # restaura o padrão

func test_balada_keeps_ecos_on_death() -> void:
	GameConfig.difficulty = GameConfig.Difficulty.BALADA
	assert_true(GameConfig.current_rules()["keep_ecos"])

func test_cancao_drops_ecos_on_death() -> void:
	GameConfig.difficulty = GameConfig.Difficulty.CANCAO
	assert_false(GameConfig.current_rules()["keep_ecos"])

func test_requiem_has_less_stamina() -> void:
	GameConfig.difficulty = GameConfig.Difficulty.REQUIEM
	assert_lt(GameConfig.current_rules()["stamina_mult"], 1.0)

func test_balada_has_wider_parry_than_requiem() -> void:
	var balada: float = GameConfig.DIFFICULTY_TABLE[GameConfig.Difficulty.BALADA]["parry_window"]
	var requiem: float = GameConfig.DIFFICULTY_TABLE[GameConfig.Difficulty.REQUIEM]["parry_window"]
	assert_gt(balada, requiem)
