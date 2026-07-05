extends Control
## Os finais de Ecos de Lirael. Após derrotar Selene, o dilema: silenciar
## o eco, completar a canção dela, ou — se reuniu as Memórias Perdidas —
## cantar a Canção do Mundo inteira (final verdadeiro).

const MEMORIES_FOR_TRUE := 3

const DILEMMA := [
	"Selene cai de joelhos. O Silêncio recua dela como maré vazante.",
	"— Faça o que veio fazer, cartógrafa. Silencie o eco... ou complete "
			+ "a canção que eu não tive coragem de terminar.",
]

const EPILOGUES := {
	"silenciar": [
		"Você silencia o eco. O Silêncio se desfaz como neblina ao sol.",
		"Lys corre para os seus braços. Pedra-Alva desperta em cores.",
		"Onde Selene esteve, resta só o vento. Você guarda o mapa dela — "
				+ "o único retrato que sobrou.",
		"FIM — \"O Preço do Silêncio\"",
	],
	"completar": [
		"Você ergue a Ocarina de Vidro e toca a nota que faltava.",
		"A canção de Selene se completa. Ela chora, sorri, e se dissolve "
				+ "em luz — enfim em paz.",
		"Lirael volta a cantar. Lys segura sua mão, e o mundo tem som de novo.",
		"FIM — \"A Canção Terminada\"",
	],
	"cancao": [
		"As Memórias Perdidas brilham na sua bolsa. Você não canta a canção "
				+ "de Selene — canta a do mundo inteiro, com ela.",
		"O luto não some: encontra companhia. Selene se levanta, viva, "
				+ "a última Guardiã ao seu lado.",
		"Lys ri. Pedra-Alva floresce. E pela primeira vez em séculos, "
				+ "ninguém em Lirael canta sozinho.",
		"FIM VERDADEIRO — \"Ecos de Lirael\"",
	],
}

@onready var text_label: Label = %TextLabel
@onready var choices: VBoxContainer = %Choices


func _ready() -> void:
	AudioManager.play_level_music("crypt")
	text_label.text = "\n\n".join(DILEMMA)
	_add_button("Silenciar o eco", _choose.bind("silenciar"))
	_add_button("Completar a canção de Selene", _choose.bind("completar"))
	if _memories_collected() >= MEMORIES_FOR_TRUE:
		_add_button("Cantar a Canção do Mundo", _choose.bind("cancao"))


func _memories_collected() -> int:
	var count := 0
	for id in GameState.inventory:
		if str(id).begins_with("memoria_"):
			count += 1
	return count


func _add_button(text: String, on_press: Callable) -> void:
	var button := Button.new()
	button.text = text
	button.add_theme_font_size_override("font_size", 11)
	button.pressed.connect(on_press)
	choices.add_child(button)


func _choose(kind: String) -> void:
	for child in choices.get_children():
		child.queue_free()
	GameState.flags["jogo_concluido"] = true
	GameState.flags["final_" + kind] = true
	SaveManager.save_game()
	AudioManager.play_level_music("heart")
	AudioManager.play_sfx("melody_return")
	text_label.text = "\n\n".join(EPILOGUES[kind])
	await get_tree().create_timer(0.6).timeout
	_add_button("Voltar ao Título", _to_title)


func _to_title() -> void:
	get_tree().change_scene_to_file("res://ui/title_screen.tscn")
