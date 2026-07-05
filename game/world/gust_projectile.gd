extends Node2D
## Rajada de vento: projétil reto da Sentinela do Vento. Some ao acertar
## a jogadora ou uma parede, ou quando expira.

@export var speed := 165.0
@export var lifetime := 1.5

var direction := Vector2.RIGHT

var _age := 0.0

@onready var sprite: Polygon2D = $Sprite


func _ready() -> void:
	$Detector.body_entered.connect(_on_body_entered)
	rotation = direction.angle()


func _process(delta: float) -> void:
	_age += delta
	if _age >= lifetime:
		queue_free()
		return
	position += direction * speed * delta
	sprite.rotation = sin(_age * 30.0) * 0.2


func _on_body_entered(_body: Node2D) -> void:
	queue_free()
