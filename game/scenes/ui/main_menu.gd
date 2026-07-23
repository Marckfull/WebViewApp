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
	subtitle.text = Locale.t("MENU_SUBTITLE")
	subtitle.position = Vector2(62, 100)
	subtitle.add_theme_color_override("font_color", Color(0.6, 0.65, 0.75))
	add_child(subtitle)

	_add_button(Locale.t("MENU_NEW_GAME"), Vector2(62, 150), _new_game)
	_continue_btn = _add_button(Locale.t("MENU_CONTINUE"), Vector2(62, 186), _continue_game)
	_continue_btn.disabled = not SaveManager.has_save(SaveManager.AUTOSAVE_SLOT)
	_add_button(Locale.t("MENU_LOAD"), Vector2(62, 222), func(): SaveSlotsMenu.open_for_load())
	_diff_btn = _add_button("%s: %s" % [Locale.t("DIFFICULTY"), DIFFICULTY_NAMES[GameConfig.difficulty]], Vector2(62, 258), _cycle_difficulty)
	_add_button(Locale.t("MENU_QUIT"), Vector2(62, 294), func(): get_tree().quit())

func _new_game() -> void:
	SaveManager.reset_state()
	get_tree().change_scene_to_file("res://scenes/ui/intro.tscn")

func _continue_game() -> void:
	if not SaveManager.load_game(SaveManager.AUTOSAVE_SLOT):
		return
	var scene := String(SaveManager.state.get("current_scene", ""))
	if scene == "":
		scene = GAME_SCENE
	else:
		var pos: Array = SaveManager.state["aria"].get("position", [0.0, 0.0])
		GameConfig.next_spawn = Vector2(pos[0], pos[1])
		GameConfig.has_next_spawn = true
	get_tree().change_scene_to_file(scene)

func _cycle_difficulty() -> void:
	var next := (int(GameConfig.difficulty) + 1) % DIFFICULTY_NAMES.size()
	GameConfig.difficulty = next
	_diff_btn.text = "%s: %s" % [Locale.t("DIFFICULTY"), DIFFICULTY_NAMES[next]]

func _add_button(text: String, pos: Vector2, cb: Callable) -> Button:
	var b := Button.new()
	b.text = text
	b.position = pos
	b.custom_minimum_size = Vector2(220, 32)
	b.pressed.connect(cb)
	add_child(b)
	return b
