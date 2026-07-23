extends Node
## ShrineMenu — menu de gasto de Ecos em atributos, nos santuários (§3.3).
##
## Autoload com UI construída por código (padrão do DialogueManager/OcarinaManager).
## Sobe os 5 atributos: Vitalidade→vida, Stamina→vigor, Força→dano, Destreza→custo
## de stamina, Harmonia→janela de parry (todos com efeito de combate, §3.3). Também
## abre a tela de equipar (arma/armadura/amuleto).

const LEVELABLE := ["vitalidade", "stamina", "forca", "destreza", "harmonia"]
const LABELS := {
	"vitalidade": "Vitalidade (+HP)",
	"stamina": "Stamina (+vigor)",
	"forca": "Força (+dano)",
	"destreza": "Destreza (-custo)",
	"harmonia": "Harmonia (+parry)",
}

var _layer: CanvasLayer
var _ecos_label: Label
var _cost_label: Label
var _buttons: Dictionary = {}
var _open: bool = false

func _ready() -> void:
	_build_ui()

func open() -> void:
	_open = true
	GameConfig.gameplay_locked = true
	_refresh()
	_layer.visible = true

func close() -> void:
	_open = false
	_layer.visible = false
	GameConfig.gameplay_locked = false

func _spend(attr_key: String) -> void:
	var attrs: Dictionary = SaveManager.state["attributes"]
	var ecos: int = SaveManager.state["aria"]["ecos"]
	var result := Attributes.try_level_up(attr_key, attrs, ecos)
	if not result["ok"]:
		return
	SaveManager.state["attributes"] = result["attrs"]
	SaveManager.state["aria"]["ecos"] = result["ecos"]
	GameEvents.ecos_changed.emit(result["ecos"])
	# Aplica ao jogador imediatamente.
	var player := get_tree().get_first_node_in_group("player")
	if player and player.has_method("apply_attributes"):
		player.apply_attributes()
	SaveManager.save_game()
	_refresh()

func _refresh() -> void:
	var attrs: Dictionary = SaveManager.state["attributes"]
	var ecos: int = SaveManager.state["aria"]["ecos"]
	var cost := Attributes.cost_for_total_level(Attributes.total_level(attrs))
	_ecos_label.text = "Ecos: %d" % ecos
	_cost_label.text = "Próximo ponto: %d Ecos" % cost
	for key in LEVELABLE:
		var lvl := int(attrs.get(key, 1))
		_buttons[key].text = "%s — Nv %d" % [LABELS[key], lvl]
		_buttons[key].disabled = ecos < cost

func _build_ui() -> void:
	_layer = CanvasLayer.new()
	_layer.layer = 11
	_layer.visible = false
	add_child(_layer)

	var panel := Panel.new()
	panel.set_anchors_preset(Control.PRESET_CENTER)
	panel.custom_minimum_size = Vector2(300, 336)
	panel.size = Vector2(300, 336)
	panel.position = Vector2(-150, -168)
	_layer.add_child(panel)

	var title := Label.new()
	title.position = Vector2(12, 8)
	title.text = "Santuário do Eco"
	title.add_theme_color_override("font_color", Color(0.95, 0.8, 0.4))
	panel.add_child(title)

	_ecos_label = Label.new()
	_ecos_label.position = Vector2(12, 30)
	panel.add_child(_ecos_label)

	_cost_label = Label.new()
	_cost_label.position = Vector2(12, 50)
	panel.add_child(_cost_label)

	var y := 78
	for key in LEVELABLE:
		var b := Button.new()
		b.position = Vector2(12, y)
		b.custom_minimum_size = Vector2(276, 30)
		var k := key
		b.pressed.connect(func(): _spend(k))
		panel.add_child(b)
		_buttons[key] = b
		y += 36

	var equip_btn := Button.new()
	equip_btn.text = "Equipamento"
	equip_btn.position = Vector2(12, y + 2)
	equip_btn.custom_minimum_size = Vector2(276, 28)
	equip_btn.pressed.connect(_open_equip)
	panel.add_child(equip_btn)

	var close_btn := Button.new()
	close_btn.text = "Descansar e sair"
	close_btn.position = Vector2(12, y + 34)
	close_btn.custom_minimum_size = Vector2(276, 28)
	close_btn.pressed.connect(close)
	panel.add_child(close_btn)

## Abre a tela de equipar por cima do santuário (fecha ao voltar).
func _open_equip() -> void:
	_layer.visible = false
	EquipMenu.open(_reopen)

## Callback ao fechar a tela de equipar: volta ao santuário.
func _reopen() -> void:
	_layer.visible = true
	_refresh()
