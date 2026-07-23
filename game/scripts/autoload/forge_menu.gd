extends Node
## ForgeMenu — a forja da Mestra Odara (autoload, §3.3). Gasta Ecos + minério para
## subir o nível da arma equipada (mais dano). UI construída por código.

var _layer: CanvasLayer
var _title: Label
var _cost: Label
var _upgrade_btn: Button
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

func _player() -> Node:
	return get_tree().get_first_node_in_group("player")

func _upgrade() -> void:
	var p := _player()
	if p == null or p.equipped_weapon == null:
		return
	var wid := String(p.equipped_weapon.id)
	var levels: Dictionary = SaveManager.state["weapon_levels"]
	var level := int(levels.get(wid, 0))
	var ecos: int = SaveManager.state["aria"]["ecos"]
	var tier := WeaponUpgrade.required_tier(level)
	var ore := Consumables.count(tier, SaveManager.state["resources"])
	if not WeaponUpgrade.can_upgrade(level, ecos, ore):
		return
	SaveManager.state["aria"]["ecos"] = ecos - WeaponUpgrade.ecos_cost(level)
	SaveManager.state["resources"][String(tier)] = ore - WeaponUpgrade.minerio_cost(level)
	levels[wid] = level + 1
	GameEvents.ecos_changed.emit(SaveManager.state["aria"]["ecos"])
	if p.has_method("refresh_weapon_label"):
		p.refresh_weapon_label()
	SaveManager.save_game()
	_refresh()

func _refresh() -> void:
	var p := _player()
	if p == null or p.equipped_weapon == null:
		_title.text = "Sem arma equipada"
		_cost.text = ""
		_upgrade_btn.disabled = true
		return
	var wid := String(p.equipped_weapon.id)
	var level := int(SaveManager.state["weapon_levels"].get(wid, 0))
	var ecos: int = SaveManager.state["aria"]["ecos"]
	_title.text = "%s  +%d" % [p.equipped_weapon.display_name, level]
	if level >= WeaponUpgrade.MAX_LEVEL:
		_cost.text = "Nível máximo."
		_upgrade_btn.disabled = true
	else:
		var tier := WeaponUpgrade.required_tier(level)
		var ore := Consumables.count(tier, SaveManager.state["resources"])
		_cost.text = "Custo: %d Ecos + %d %s  (você: %d / %d)" % [
			WeaponUpgrade.ecos_cost(level), WeaponUpgrade.minerio_cost(level),
			WeaponUpgrade.tier_label(tier), ecos, ore]
		_upgrade_btn.disabled = not WeaponUpgrade.can_upgrade(level, ecos, ore)

func _build_ui() -> void:
	_layer = CanvasLayer.new()
	_layer.layer = 12
	_layer.visible = false
	add_child(_layer)

	var panel := Panel.new()
	panel.set_anchors_preset(Control.PRESET_CENTER)
	panel.custom_minimum_size = Vector2(340, 150)
	panel.size = Vector2(340, 150)
	panel.position = Vector2(-170, -75)
	_layer.add_child(panel)

	var head := Label.new()
	head.position = Vector2(12, 8)
	head.text = "Forja da Mestra Odara"
	head.add_theme_color_override("font_color", Color(0.95, 0.7, 0.4))
	panel.add_child(head)

	_title = Label.new()
	_title.position = Vector2(12, 36)
	panel.add_child(_title)

	_cost = Label.new()
	_cost.position = Vector2(12, 60)
	_cost.add_theme_color_override("font_color", Color(0.8, 0.82, 0.86))
	panel.add_child(_cost)

	_upgrade_btn = Button.new()
	_upgrade_btn.position = Vector2(12, 92)
	_upgrade_btn.custom_minimum_size = Vector2(150, 30)
	_upgrade_btn.text = "Forjar (+1)"
	_upgrade_btn.pressed.connect(_upgrade)
	panel.add_child(_upgrade_btn)

	var close_btn := Button.new()
	close_btn.position = Vector2(172, 92)
	close_btn.custom_minimum_size = Vector2(150, 30)
	close_btn.text = "Sair"
	close_btn.pressed.connect(close)
	panel.add_child(close_btn)
