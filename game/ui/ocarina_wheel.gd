extends CanvasLayer
## Roda de melodias da Ocarina de Vidro: pausa o jogo, toca a melodia
## escolhida e emite o efeito pelo event bus.

var _open := false

@onready var return_button: Button = %ReturnButton
@onready var calm_button: Button = %CalmButton


func _ready() -> void:
	visible = false
	return_button.pressed.connect(_play.bind("retorno", "melody_return"))
	calm_button.pressed.connect(_play.bind("acalento", "melody_calm"))


func _process(_delta: float) -> void:
	if Input.is_action_just_pressed("ocarina"):
		_toggle()


func _toggle() -> void:
	# Não abre por cima de outro modal (diálogo/bolsa).
	if not _open and get_tree().paused:
		return
	if not _open and not GameState.has_item("ocarina_vidro"):
		GameEvents.notify("Você não carrega uma ocarina.")
		return
	_open = not _open
	visible = _open
	get_tree().paused = _open
	AudioManager.play_sfx("blip")


func _play(melody_id: String, sfx_name: String) -> void:
	_open = false
	visible = false
	get_tree().paused = false
	AudioManager.play_sfx(sfx_name)
	GameEvents.melody_played.emit(melody_id)
