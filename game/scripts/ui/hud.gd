extends Control
## HUD — barras de vida/stamina, contador de Ecos e botões de ação touch.
##
## Fase 0: construído por código para manter a cena enxuta. Barras seguem os
## sinais do GameEvents (desacoplado). Botões alimentam as mesmas ações que o
## teclado/gamepad, disparando Input.action_press/release.

var _hp: ProgressBar
var _stamina: ProgressBar
var _ecos: Label
var _flasks: Label
var _boss_bar: ProgressBar
var _boss_label: Label
var _weapon: Label
var _vigor: Label

func _ready() -> void:
	set_anchors_preset(Control.PRESET_FULL_RECT)
	mouse_filter = Control.MOUSE_FILTER_IGNORE
	_build_bars()
	_build_boss_bar()
	_build_buttons()
	GameEvents.health_changed.connect(_on_health)
	GameEvents.stamina_changed.connect(_on_stamina)
	GameEvents.ecos_changed.connect(_on_ecos)
	GameEvents.flasks_changed.connect(_on_flasks)
	GameEvents.boss_spawned.connect(_on_boss_spawned)
	GameEvents.boss_health_changed.connect(_on_boss_health)
	GameEvents.boss_defeated.connect(_on_boss_defeated)
	GameEvents.weapon_changed.connect(_on_weapon)
	GameEvents.consumable_changed.connect(_on_consumable)

func _build_bars() -> void:
	_hp = _make_bar(Color(0.9, 0.25, 0.3), Vector2(12, 10))
	_stamina = _make_bar(Color(0.3, 0.8, 0.5), Vector2(12, 26))
	_ecos = Label.new()
	_ecos.position = Vector2(12, 42)
	_ecos.text = "Ecos: 0"
	add_child(_ecos)
	_flasks = Label.new()
	_flasks.position = Vector2(12, 60)
	_flasks.add_theme_color_override("font_color", Color(0.6, 0.9, 0.7))
	_flasks.text = "Frascos: 3"
	add_child(_flasks)
	_weapon = Label.new()
	_weapon.position = Vector2(12, 78)
	_weapon.add_theme_color_override("font_color", Color(0.85, 0.8, 0.6))
	_weapon.text = "Arma: —"
	add_child(_weapon)
	_vigor = Label.new()
	_vigor.position = Vector2(12, 96)
	_vigor.add_theme_color_override("font_color", Color(0.4, 0.8, 0.55))
	_vigor.text = "Vigor: %d" % int(SaveManager.state["consumables"].get("pocao_vigor", 0))
	add_child(_vigor)

func _make_bar(color: Color, pos: Vector2) -> ProgressBar:
	var bar := ProgressBar.new()
	bar.custom_minimum_size = Vector2(120, 12)
	bar.position = pos
	bar.max_value = 100.0
	bar.value = 100.0
	bar.show_percentage = false
	var sb := StyleBoxFlat.new()
	sb.bg_color = color
	bar.add_theme_stylebox_override("fill", sb)
	add_child(bar)
	return bar

## Barra de vida do boss no topo — só aparece durante o encontro (§3.2).
func _build_boss_bar() -> void:
	_boss_label = Label.new()
	_boss_label.anchor_left = 0.5
	_boss_label.anchor_right = 0.5
	_boss_label.position = Vector2(-120, 8)
	_boss_label.custom_minimum_size = Vector2(240, 0)
	_boss_label.horizontal_alignment = HORIZONTAL_ALIGNMENT_CENTER
	_boss_label.visible = false
	add_child(_boss_label)

	_boss_bar = ProgressBar.new()
	_boss_bar.anchor_left = 0.5
	_boss_bar.anchor_right = 0.5
	_boss_bar.position = Vector2(-160, 28)
	_boss_bar.custom_minimum_size = Vector2(320, 10)
	_boss_bar.show_percentage = false
	_boss_bar.max_value = 100.0
	_boss_bar.value = 100.0
	var sb := StyleBoxFlat.new()
	sb.bg_color = Color(0.75, 0.2, 0.55)
	_boss_bar.add_theme_stylebox_override("fill", sb)
	_boss_bar.visible = false
	add_child(_boss_bar)

func _on_boss_spawned(boss_name: String, max_hp: float) -> void:
	_boss_label.text = boss_name
	_boss_label.visible = true
	_boss_bar.max_value = max_hp
	_boss_bar.value = max_hp
	_boss_bar.visible = true

func _on_boss_health(current: float, _maximum: float) -> void:
	_boss_bar.value = current

func _on_boss_defeated(_id: StringName) -> void:
	_boss_bar.visible = false
	_boss_label.visible = false

## Botões de ação no canto inferior direito (layout do §3.1: customizável em prod).
func _build_buttons() -> void:
	var actions := [
		["ATK", "attack", Vector2(-70, -70)],
		["DODGE", "dodge", Vector2(-140, -40)],
		["GUARD", "guard", Vector2(-40, -140)],
		["LOCK", "lock_on", Vector2(-140, -110)],
		["USAR", "interact", Vector2(-210, -70)],
		["HEAL", "heal", Vector2(-210, -140)],
		["SWAP", "swap_weapon", Vector2(-210, -210)],
		["VIGOR", "consumable", Vector2(-280, -140)],
	]
	for a in actions:
		var btn := Button.new()
		btn.text = a[0]
		btn.custom_minimum_size = Vector2(60, 60)
		btn.set_anchors_preset(Control.PRESET_BOTTOM_RIGHT)
		btn.position = a[2]
		var action: StringName = a[1]
		btn.button_down.connect(func(): Input.action_press(action))
		btn.button_up.connect(func(): Input.action_release(action))
		add_child(btn)

	# Ocarina abre a roda de melodias — chama o autoload direto (não via ação).
	var ocarina_btn := Button.new()
	ocarina_btn.text = "OCARINA"
	ocarina_btn.custom_minimum_size = Vector2(70, 60)
	ocarina_btn.set_anchors_preset(Control.PRESET_BOTTOM_RIGHT)
	ocarina_btn.position = Vector2(-70, -210)
	ocarina_btn.pressed.connect(func(): OcarinaManager.toggle())
	add_child(ocarina_btn)

	# Pausa (canto superior direito).
	var pause_btn := Button.new()
	pause_btn.text = "II"
	pause_btn.custom_minimum_size = Vector2(36, 30)
	pause_btn.set_anchors_preset(Control.PRESET_TOP_RIGHT)
	pause_btn.position = Vector2(-44, 8)
	pause_btn.pressed.connect(func(): PauseMenu.toggle())
	add_child(pause_btn)

func _on_health(current: float, maximum: float) -> void:
	_hp.max_value = maximum
	_hp.value = current

func _on_stamina(current: float, maximum: float) -> void:
	_stamina.max_value = maximum
	_stamina.value = current

func _on_ecos(total: int) -> void:
	_ecos.text = "Ecos: %d" % total

func _on_flasks(current: int, _maximum: int) -> void:
	_flasks.text = "Frascos: %d" % current

func _on_weapon(display_name: String) -> void:
	_weapon.text = "Arma: %s" % display_name

func _on_consumable(id: StringName, count: int) -> void:
	if id == &"pocao_vigor":
		_vigor.text = "Vigor: %d" % count
