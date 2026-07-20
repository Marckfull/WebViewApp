class_name StaminaComponent
extends Node
## Stamina — o coração do combate souls-like (§3.2).
##
## Ataque, esquiva e defesa consomem stamina; gerenciá-la é o núcleo do combate.
## Regenera rápido fora de ação, com um pequeno atraso após gastar.

@export var max_stamina: float = 100.0
@export var regen_per_second: float = 45.0
@export var regen_delay: float = 0.5   ## atraso antes de começar a regenerar

var current: float
var _regen_cooldown: float = 0.0

func _ready() -> void:
	# Aplica o multiplicador de dificuldade (§3.4).
	max_stamina *= GameConfig.current_rules()["stamina_mult"]
	current = max_stamina

func _process(delta: float) -> void:
	if _regen_cooldown > 0.0:
		_regen_cooldown -= delta
		return
	if current < max_stamina:
		current = minf(current + regen_per_second * delta, max_stamina)
		GameEvents.stamina_changed.emit(current, max_stamina)

## Retorna true se havia stamina suficiente e o custo foi consumido.
func try_spend(amount: float) -> bool:
	if current < amount:
		return false
	current -= amount
	_regen_cooldown = regen_delay
	GameEvents.stamina_changed.emit(current, max_stamina)
	return true

func has_at_least(amount: float) -> bool:
	return current >= amount

## Ajusta a stamina máxima (ex.: subir o atributo Stamina, §3.3), já aplicando o
## multiplicador de dificuldade. Recompleta a stamina.
func set_max(base_value: float) -> void:
	max_stamina = base_value * GameConfig.current_rules()["stamina_mult"]
	current = max_stamina
	GameEvents.stamina_changed.emit(current, max_stamina)
