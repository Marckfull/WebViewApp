extends Area2D
## Forja da Mestra Odara: gasta Minério de Eco para reforjar a lâmina
## de Aria (+dano por nível, até o nível máximo).

const COST := 3
const MAX_LEVEL := 3

var _player: Player

@onready var prompt: Label = $Prompt


func _ready() -> void:
	body_entered.connect(_on_body_entered)
	body_exited.connect(_on_body_exited)
	GameState.inventory_changed.connect(_update_prompt)
	GameState.weapon_changed.connect(func(_level: int) -> void: _update_prompt())
	prompt.visible = false
	_update_prompt()


func _physics_process(_delta: float) -> void:
	if _player and _player.is_alive() and Input.is_action_just_pressed("interact"):
		_try_forge()


func _try_forge() -> void:
	if GameState.weapon_level >= MAX_LEVEL:
		GameEvents.notify("A lâmina já canta no limite do metal.")
		return
	if GameState.item_count("minerio_eco") < COST:
		GameEvents.notify("Odara precisa de %d Minérios de Eco." % COST)
		return
	GameState.remove_item("minerio_eco", COST)
	GameState.upgrade_weapon()
	AudioManager.play_sfx("forge")
	GameEvents.notify("A lâmina canta mais alto! (forja nível %d)" % GameState.weapon_level)


func _update_prompt() -> void:
	if GameState.weapon_level >= MAX_LEVEL:
		prompt.text = "A forja descansa."
	else:
		prompt.text = "[E / USAR] Reforjar — %d Minério de Eco (tem %d)" \
				% [COST, GameState.item_count("minerio_eco")]


func _on_body_entered(body: Node2D) -> void:
	if body is Player:
		_player = body
		prompt.visible = true


func _on_body_exited(body: Node2D) -> void:
	if body == _player:
		_player = null
		prompt.visible = false
