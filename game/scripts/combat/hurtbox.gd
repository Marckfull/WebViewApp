class_name Hurtbox
extends Area2D
## Hurtbox — área que recebe dano e o repassa ao HealthComponent do dono.
##
## Durante a esquiva (§3.2), o Player desliga esta hurtbox por N frames para
## simular os i-frames — rolar na direção certa atravessa o ataque. O parry é
## resolvido antes: se guard estiver ativo dentro da janela, converte em crítico.

signal hit_taken(hitbox: Hitbox)

@export var health_component_path: NodePath
@export var invulnerable: bool = false   ## ligado durante i-frames de esquiva
@export var guarding: bool = false        ## escudo erguido (§3.1)
@export var parry_active: bool = false    ## janela de parry aberta (§3.2)

var _health: HealthComponent

func _ready() -> void:
	if health_component_path:
		_health = get_node_or_null(health_component_path) as HealthComponent

func receive_hit(hitbox: Hitbox) -> void:
	if invulnerable:
		return  ## i-frames: atravessou o ataque

	if parry_active:
		# Parry no tempo certo abre o atacante para crítico (§3.2).
		GameEvents.parry_success.emit(hitbox.get_parent())
		return

	var dmg := hitbox.damage
	var poise_dmg := hitbox.poise_damage
	if guarding:
		# Defesa reduz dano mas ainda pressiona a postura.
		dmg *= 0.25
		poise_dmg *= 0.5

	if _health:
		_health.take_damage(dmg, poise_dmg)
	hit_taken.emit(hitbox)
