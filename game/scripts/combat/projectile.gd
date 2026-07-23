class_name Projectile
extends Hitbox
## Projectile — golpe à distância (§3.3). Estende Hitbox: reusa dano/postura e o
## par Hitbox/Hurtbox. Move em linha reta, some ao acertar uma Hurtbox ou ao fim
## do tempo de vida. Quem dispara (arco de Aria, inimigo ranged) define direção,
## dano e as camadas de colisão via setup().

@export var speed: float = 260.0
@export var lifetime: float = 2.0

var _dir: Vector2 = Vector2.RIGHT
var _life: float = 0.0

## Configura o projétil antes de adicioná-lo à cena.
func setup(dir: Vector2, dmg: float, poise: float, layer: int, mask: int) -> void:
	_dir = dir.normalized() if dir != Vector2.ZERO else Vector2.RIGHT
	damage = dmg
	poise_damage = poise
	collision_layer = layer
	collision_mask = mask
	rotation = _dir.angle()

func _ready() -> void:
	super._ready()          ## Hitbox liga area_entered (aplica dano) e monitoring=false
	monitoring = true       ## projétil já nasce ativo
	area_entered.connect(_on_projectile_hit)

func _physics_process(delta: float) -> void:
	global_position += _dir * speed * delta
	_life += delta
	if _life >= lifetime:
		queue_free()

## O dano já foi aplicado pelo handler da Hitbox; aqui só consumimos o projétil.
func _on_projectile_hit(area: Area2D) -> void:
	if area is Hurtbox:
		queue_free()
