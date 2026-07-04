extends CPUParticles2D
## Faíscas de impacto de um disparo só: emite e some.


func _ready() -> void:
	finished.connect(queue_free)
	emitting = true
