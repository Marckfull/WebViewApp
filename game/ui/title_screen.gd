extends Control
## Tela de título: Continuar (se houver save) ou Novo Jogo.

@onready var continue_button: Button = %ContinueButton
@onready var new_game_button: Button = %NewGameButton


func _ready() -> void:
	continue_button.visible = SaveManager.has_save()
	continue_button.pressed.connect(_on_continue_pressed)
	new_game_button.pressed.connect(_on_new_game_pressed)
	if continue_button.visible:
		continue_button.grab_focus()
	else:
		new_game_button.grab_focus()
	AudioManager.play_level_music("village")


func _on_continue_pressed() -> void:
	AudioManager.play_sfx("blip")
	if SaveManager.load_game():
		get_tree().change_scene_to_file(GameState.last_shrine_scene)


func _on_new_game_pressed() -> void:
	AudioManager.play_sfx("blip")
	SaveManager.delete_save()
	GameState.reset()
	get_tree().change_scene_to_file("res://world/village.tscn")
