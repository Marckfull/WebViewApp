class_name EnemyData
extends Resource
## EnemyData — inimigo data-driven (§6.2). Permite o "remix de inimigos" barato
## do New Game+ (§3.6) e o bestiário automático (§3.6): trocar stats via .tres.

@export var id: StringName
@export var display_name: String = "Ecoado"
@export var max_health: float = 60.0
@export var max_poise: float = 40.0
@export var contact_damage: float = 8.0
@export var move_speed: float = 45.0
@export var ecos_reward: int = 15             ## moeda dropada ao morrer (§3.2)
@export var nocturnal: bool = false            ## inimigos diferentes à noite (§3.5)
@export var tint: Color = Color(0.85, 0.3, 0.35) ## cor do greybox (variedade visual)
@export_multiline var bestiary_note: String = ""
