extends Control
## Escolha de dificuldade ao iniciar um Novo Jogo. Define o modo antes
## da abertura.

const OPTIONS := [
	{
		"id": 0, "name": "Balada",
		"desc": "Para viver a história. 5 frascos, parry generoso, e você "
				+ "conserva os Ecos ao cair.",
	},
	{
		"id": 1, "name": "Canção",
		"desc": "A experiência pretendida. Desafio justo — 3 frascos, "
				+ "punição por erro.",
	},
	{
		"id": 2, "name": "Requiem",
		"desc": "Souls puro. Inimigos batem mais forte, só 2 frascos e a "
				+ "janela do parry é apertada.",
	},
]

@onready var options_box: VBoxContainer = %Options


func _ready() -> void:
	AudioManager.play_level_music("village")
	for i in OPTIONS.size():
		var opt: Dictionary = OPTIONS[i]
		var row := _build_row(opt)
		options_box.add_child(row)
		if i == 1:
			(row.get_node("Choose") as Button).grab_focus()


func _build_row(opt: Dictionary) -> Control:
	var box := VBoxContainer.new()
	box.add_theme_constant_override("separation", 2)
	var button := Button.new()
	button.name = "Choose"
	button.text = opt["name"]
	button.add_theme_font_size_override("font_size", 14)
	button.pressed.connect(_choose.bind(int(opt["id"])))
	box.add_child(button)
	var desc := Label.new()
	desc.text = opt["desc"]
	desc.autowrap_mode = TextServer.AUTOWRAP_WORD_SMART
	desc.add_theme_font_size_override("font_size", 8)
	desc.add_theme_color_override("font_color", Color(0.62, 0.64, 0.72))
	box.add_child(desc)
	return box


func _choose(id: int) -> void:
	AudioManager.play_sfx("blip")
	SaveManager.delete_save()
	GameState.reset()
	GameState.difficulty = id
	GameState.flasks_max = GameState.flasks_for_difficulty()
	GameState.flasks = GameState.flasks_max
	get_tree().change_scene_to_file("res://ui/intro.tscn")
