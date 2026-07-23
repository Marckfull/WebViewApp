extends Area2D
## EscortGoal — zona de destino de uma missão ESCORT (§3.5). Detecta o NPC do
## grupo "escort" e chama arrive() nele, concluindo a escolta.

func _ready() -> void:
	body_entered.connect(_on_body_entered)

func _on_body_entered(body: Node) -> void:
	if body.is_in_group("escort") and body.has_method("arrive"):
		body.arrive()
