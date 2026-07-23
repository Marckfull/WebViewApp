extends CPUParticles2D
## HitSpark — faísca de impacto de vida curta (§5). Emite uma vez e se remove.

func _ready() -> void:
	emitting = true
	finished.connect(queue_free)
