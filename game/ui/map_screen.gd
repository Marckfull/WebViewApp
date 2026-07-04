extends CanvasLayer
## Mapa da Cartógrafa: Aria desenha o mapa por onde passa. Mostra as
## células visitadas da área atual, além de santuário, portais e a
## própria posição.

const VISITED_COLOR := Color(0.85, 0.78, 0.6, 0.9)
const SHRINE_COLOR := Color(0.95, 0.8, 0.35)
const PORTAL_COLOR := Color(0.5, 0.85, 0.95)
const PLAYER_COLOR := Color(0.3, 0.9, 0.75)

var _open := false

@onready var map_view: Control = %MapView


func _ready() -> void:
	visible = false
	map_view.draw.connect(_on_map_draw)


func _process(_delta: float) -> void:
	if Input.is_action_just_pressed("map"):
		_toggle()


func _toggle() -> void:
	# Não abre por cima de outro modal (diálogo/bolsa/ocarina).
	if not _open and get_tree().paused:
		return
	_open = not _open
	visible = _open
	get_tree().paused = _open
	AudioManager.play_sfx("blip")
	if _open:
		map_view.queue_redraw()


@warning_ignore("integer_division")
func _on_map_draw() -> void:
	var scene := get_tree().current_scene
	if scene == null:
		return
	var data: Dictionary = GameState.map_data.get(scene.scene_file_path, {})
	if data.is_empty():
		return
	var cols: int = data["cols"]
	var rows: int = data["rows"]
	var cell := minf(map_view.size.x / cols, map_view.size.y / rows)
	var origin := (map_view.size - Vector2(cols, rows) * cell) / 2.0
	for c in data["cells"]:
		var idx := int(c)
		var pos := Vector2(idx % cols, idx / cols) * cell
		map_view.draw_rect(
				Rect2(origin + pos, Vector2(cell - 1.0, cell - 1.0)),
				VISITED_COLOR)
	var scale_factor := cell / GameState.MAP_CELL
	var shrine := scene.get_node_or_null("Shrine")
	if shrine:
		map_view.draw_circle(
				origin + shrine.global_position * scale_factor, 3.0, SHRINE_COLOR)
	for portal in get_tree().get_nodes_in_group("portals"):
		map_view.draw_circle(
				origin + portal.global_position * scale_factor, 2.5, PORTAL_COLOR)
	var player := get_tree().get_first_node_in_group("player") as Node2D
	if player:
		map_view.draw_circle(
				origin + player.global_position * scale_factor, 3.0, PLAYER_COLOR)
