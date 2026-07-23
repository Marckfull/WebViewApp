extends "res://addons/gut/test.gd"
## Testa a dica diegética (§4): revela o texto quando o player entra e o esconde
## quando sai. Sem menu, sem pausa.

var scene: PackedScene

func before_all() -> void:
	scene = load("res://scenes/world/tutorial_hint.tscn")

func test_scene_loads() -> void:
	assert_not_null(scene, "tutorial_hint.tscn deve carregar")

func test_label_hidden_until_player_near() -> void:
	var hint = scene.instantiate()
	hint.text = "Toque em ATACAR"
	add_child_autofree(hint)  # dispara _ready (configura o texto e oculta)
	var label: Label = hint.get_node("Label")
	assert_eq(label.text, "Toque em ATACAR", "texto configurado via export")
	assert_false(label.visible, "dica começa oculta")

func test_shows_and_hides_for_player() -> void:
	var hint = scene.instantiate()
	add_child_autofree(hint)
	var label: Label = hint.get_node("Label")

	# Um corpo no grupo "player" liga a dica; ao sair, desliga.
	var player := Node2D.new()
	player.add_to_group("player")
	hint._on_body_entered(player)
	assert_true(label.visible, "aparece quando Aria se aproxima")
	hint._on_body_exited(player)
	assert_false(label.visible, "some quando Aria se afasta")
	player.free()

func test_ignores_non_player_bodies() -> void:
	var hint = scene.instantiate()
	add_child_autofree(hint)
	var label: Label = hint.get_node("Label")
	var other := Node2D.new()  # sem grupo "player"
	hint._on_body_entered(other)
	assert_false(label.visible, "outros corpos não disparam a dica")
	other.free()
