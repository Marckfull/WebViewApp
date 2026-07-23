extends Control
## ThreatIndicator — setas na borda da tela apontando para inimigos que atacam
## FORA da visão (§3.6, acessibilidade). Aria é surda de um ouvido: o jogo mostra
## visualmente a ameaça que, no som, ela não captaria. Exemplar por design.

const MARGIN := 12.0
const COLOR := Color(1.0, 0.4, 0.2, 0.9)

func _ready() -> void:
	mouse_filter = Control.MOUSE_FILTER_IGNORE

func _process(_delta: float) -> void:
	queue_redraw()

func _draw() -> void:
	var xform := get_viewport().get_canvas_transform()
	var vp := size
	if vp.x <= 0.0 or vp.y <= 0.0:
		return
	var rect := Rect2(Vector2(MARGIN, MARGIN), vp - Vector2(MARGIN * 2.0, MARGIN * 2.0))
	var center := vp * 0.5
	for e in get_tree().get_nodes_in_group("enemies"):
		if not (e is Node2D) or not e.has_method("is_threatening"):
			continue
		if not e.is_threatening():
			continue
		var sp: Vector2 = xform * (e as Node2D).global_position
		if rect.has_point(sp):
			continue  # ameaça já visível na tela
		var dir := sp - center
		if dir.length() < 1.0:
			continue
		dir = dir.normalized()
		var edge := Vector2(
			clampf(sp.x, rect.position.x, rect.end.x),
			clampf(sp.y, rect.position.y, rect.end.y))
		_draw_arrow(edge, dir)

func _draw_arrow(pos: Vector2, dir: Vector2) -> void:
	var a := dir.angle()
	var p1 := pos + Vector2(9, 0).rotated(a)
	var p2 := pos + Vector2(-6, 6).rotated(a)
	var p3 := pos + Vector2(-6, -6).rotated(a)
	draw_colored_polygon(PackedVector2Array([p1, p2, p3]), COLOR)
