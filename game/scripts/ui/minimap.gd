extends Control
## MiniMap — o mapa que se desenha conforme Aria explora (§3.5). Ela é cartógrafa:
## a mecânica é temática. Fog-of-war por células reveladas ao redor dela, PERSISTIDO
## por sala no save (exploração é lembrada entre visitas e sessões).

const CELL_WORLD := 40.0     ## tamanho de cada célula do mapa em unidades de mundo
const REVEAL_RADIUS := 1     ## células reveladas ao redor de Aria

var _world: GameWorld
var _player: Node2D
var _room: Rect2 = Rect2(0, 0, 800, 480)
var _revealed: Dictionary = {}
var _scene_key: String = ""

func _ready() -> void:
	mouse_filter = Control.MOUSE_FILTER_IGNORE
	var cs := get_tree().current_scene
	_scene_key = cs.scene_file_path if cs else ""
	_load_revealed()

func _load_revealed() -> void:
	var maps: Dictionary = SaveManager.state["world"].get("map", {})
	for k in maps.get(_scene_key, []):
		_revealed[k] = true

func _process(_delta: float) -> void:
	if _world == null:
		_world = get_tree().get_first_node_in_group("world") as GameWorld
		if _world:
			_room = _world.room_bounds
	if _player == null:
		_player = get_tree().get_first_node_in_group("player") as Node2D
	if _player:
		_reveal_around(_player.global_position)
	queue_redraw()

func _reveal_around(pos: Vector2) -> void:
	var c := int(floor((pos.x - _room.position.x) / CELL_WORLD))
	var r := int(floor((pos.y - _room.position.y) / CELL_WORLD))
	var changed := false
	for dc in range(-REVEAL_RADIUS, REVEAL_RADIUS + 1):
		for dr in range(-REVEAL_RADIUS, REVEAL_RADIUS + 1):
			var key := "%d,%d" % [c + dc, r + dr]
			if not _revealed.has(key):
				_revealed[key] = true
				changed = true
	if changed:
		_persist()

func _persist() -> void:
	var maps: Dictionary = SaveManager.state["world"].get("map", {})
	maps[_scene_key] = _revealed.keys()
	SaveManager.state["world"]["map"] = maps

func _draw() -> void:
	var box := get_size()
	if _room.size.x <= 0.0 or _room.size.y <= 0.0:
		return
	var sx := box.x / _room.size.x
	var sy := box.y / _room.size.y
	var cw := CELL_WORLD * sx
	var ch := CELL_WORLD * sy
	draw_rect(Rect2(Vector2.ZERO, box), Color(0.05, 0.06, 0.09, 0.85))
	# Células reveladas.
	for key in _revealed:
		var parts := String(key).split(",")
		if parts.size() < 2:
			continue
		var p := Vector2(int(parts[0]) * cw, int(parts[1]) * ch)
		draw_rect(Rect2(p, Vector2(cw + 0.5, ch + 0.5)), Color(0.3, 0.34, 0.42, 0.9))
	# Pontos de interesse já descobertos (santuários, portas, itens).
	for node in get_tree().get_nodes_in_group("interactables"):
		if node is Node2D and (node as CanvasItem).visible:
			var gp: Vector2 = (node as Node2D).global_position
			var cc := int(floor((gp.x - _room.position.x) / CELL_WORLD))
			var cr := int(floor((gp.y - _room.position.y) / CELL_WORLD))
			if _revealed.has("%d,%d" % [cc, cr]):
				var m := gp - _room.position
				draw_circle(Vector2(m.x * sx, m.y * sy), 1.5, Color(0.95, 0.75, 0.3))
	# Aria.
	if _player:
		var pp := _player.global_position - _room.position
		draw_circle(Vector2(pp.x * sx, pp.y * sy), 2.0, Color(0.4, 0.85, 1.0))
	draw_rect(Rect2(Vector2.ZERO, box), Color(0.5, 0.55, 0.65), false, 1.0)
