extends Control
## Menu principal — Novo Jogo / Continuar / Dificuldade / Sair (§3.6).
## Cena inicial do jogo (run/main_scene). UI construída por código.

const GAME_SCENE := "res://scenes/world/cripta_das_guardias.tscn"
const DIFFICULTY_NAMES := ["Balada", "Canção", "Requiem"]

var _continue_btn: Button
var _diff_btn: Button

func _ready() -> void:
	set_anchors_preset(Control.PRESET_FULL_RECT)
	get_tree().paused = false
	GameConfig.gameplay_locked = false
	_build()

func _build() -> void:
	var bg := ColorRect.new()
	bg.color = Color(0.06, 0.07, 0.1)
	bg.set_anchors_preset(Control.PRESET_FULL_RECT)
	bg.mouse_filter = Control.MOUSE_FILTER_IGNORE
	add_child(bg)

	var title := Label.new()
	title.text = "ECOS DE LIRAEL"
	title.position = Vector2(60, 60)
	title.add_theme_font_size_override("font_size", 32)
	title.add_theme_color_override("font_color", Color(0.4, 0.85, 1))
	add_child(title)

	var subtitle := Label.new()
	subtitle.text = "Ouça a Canção do Mundo."
	subtitle.position = Vector2(62, 100)
	subtitle.add_theme_color_override("font_color", Color(0.6, 0.65, 0.75))
	add_child(subtitle)

	_add_button("Novo Jogo", Vector2(62, 150), _new_game)
	_continue_btn = _add_button("Continuar", Vector2(62, 190), _continue_game)
	_continue_btn.disabled = not SaveManager.has_save(SaveManager.AUTOSAVE_SLOT)
	_diff_btn = _add_button("Dificuldade: %s" % DIFFICULTY_NAMES[GameConfig.difficulty], Vector2(62, 230), _cycle_difficulty)
	_add_button("Sair", Vector2(62, 270), func(): get_tree().quit())

func _new_game() -> void:
	SaveManager.reset_state()
	get_tree().change_scene_to_file(GAME_SCENE)

func _continue_game() -> void:
	if SaveManager.load_game(SaveManager.AUTOSAVE_SLOT):
		get_tree().change_scene_to_file(GAME_SCENE)

func _cycle_difficulty() -> void:
	var next := (int(GameConfig.difficulty) + 1) % DIFFICULTY_NAMES.size()
	GameConfig.difficulty = next
	_diff_btn.text = "Dificuldade: %s" % DIFFICULTY_NAMES[next]

func _add_button(text: String, pos: Vector2, cb: Callable) -> Button:
	var b := Button.new()
	b.text = text
	b.position = pos
	b.custom_minimum_size = Vector2(220, 32)
	b.pressed.connect(cb)
	add_child(b)
	return b
