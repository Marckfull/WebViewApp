extends GameWorld
## Vila de Pedra-Alva — prólogo jogável (§2, Ato 1). Herda o GameWorld (câmera,
## respawn) e roteiriza o momento em que o Silêncio leva Lys: ao fim da cutscene
## `lys_taken`, Lys desaparece e a descida à Cripta se abre (antes ficava oculta,
## para o jogador viver a vila antes de perder a irmã).

func _ready() -> void:
	super._ready()
	GameEvents.cutscene_finished.connect(_on_cutscene_finished)
	var descida := get_node_or_null("DescidaCripta")
	if descida:
		descida.hide()
		descida.monitorable = false

func _on_cutscene_finished(cutscene_id: StringName) -> void:
	if cutscene_id != &"lys_taken":
		return
	var lys := get_node_or_null("Lys")
	if lys:
		lys.queue_free()
	var descida := get_node_or_null("DescidaCripta")
	if descida:
		descida.show()
		descida.monitorable = true
