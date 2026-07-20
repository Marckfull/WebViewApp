extends Control
## HUD — barras de vida/stamina, contador de Ecos e botões de ação touch.
##
## Fase 0: construído por código para manter a cena enxuta. Barras seguem os
## sinais do GameEvents (desacoplado). Botões alimentam as mesmas ações que o
## teclado/gamepad, disparando Input.action_press/release.

var _hp: ProgressBar
var _stamina: ProgressBar
var _ecos: Label

func _ready() -> void:
	set_anchors_preset(Control.PRESET_FULL_RECT)
	mouse_filter = Control.MOUSE_FILTER_IGNORE
	_build_bars()
	_build_buttons()
	GameEvents.health_changed.connect(_on_health)
	GameEvents.stamina_changed.connect(_on_stamina)
	GameEvents.ecos_changed.connect(_on_ecos)

func _build_bars() -> void:
	_hp = _make_bar(Color(0.9, 0.25, 0.3), Vector2(12, 10))
	_stamina = _make_bar(Color(0.3, 0.8, 0.5), Vector2(12, 26))
	_ecos = Label.new()
	_ecos.position = Vector2(12, 42)
	_ecos.text = "Ecos: 0"
	add_child(_ecos)

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

## Botões de ação no canto inferior direito (layout do §3.1: customizável em prod).
func _build_buttons() -> void:
	var actions := [
		["ATK", "attack", Vector2(-70, -70)],
		["DODGE", "dodge", Vector2(-140, -40)],
		["GUARD", "guard", Vector2(-40, -140)],
		["LOCK", "lock_on", Vector2(-140, -110)],
		["OCARINA", "ocarina", Vector2(-70, -180)],
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

func _on_health(current: float, maximum: float) -> void:
	_hp.max_value = maximum
	_hp.value = current

func _on_stamina(current: float, maximum: float) -> void:
	_stamina.max_value = maximum
	_stamina.value = current

func _on_ecos(total: int) -> void:
	_ecos.text = "Ecos: %d" % total
