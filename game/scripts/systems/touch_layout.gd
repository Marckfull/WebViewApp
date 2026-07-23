class_name TouchLayout
extends RefCounted
## Layout de toque customizável (§3.6/HUD): lê as preferências de settings e as
## traduz em tamanho/posição dos botões de ação e do lado do stick virtual.
## Lógica pura — o HUD e o VirtualJoystick aplicam a partir daqui.

const MIN_SCALE := 1.0
const MAX_SCALE := 1.6
const SCALE_STEP := 0.15
const BASE_BUTTON := Vector2(60, 60)

static func is_left_handed(settings: Dictionary) -> bool:
	return bool(settings.get("touch_left_handed", false))

static func button_scale(settings: Dictionary) -> float:
	return clampf(float(settings.get("touch_scale", 1.0)), MIN_SCALE, MAX_SCALE)

## Próxima escala no ciclo (volta ao mínimo depois do máximo).
static func next_scale(current: float) -> float:
	var n := snappedf(current + SCALE_STEP, 0.01)
	return MIN_SCALE if n > MAX_SCALE + 0.001 else n

## Tamanho do botão de ação já escalado.
static func button_size(settings: Dictionary) -> Vector2:
	return BASE_BUTTON * button_scale(settings)

## Preset de âncora do cluster de ação: inferior-direito (destro) ou -esquerdo (canhoto).
static func button_preset(settings: Dictionary) -> int:
	return Control.PRESET_BOTTOM_LEFT if is_left_handed(settings) else Control.PRESET_BOTTOM_RIGHT

## Converte um offset base (ancorado ao canto inferior-direito, x negativo) para o
## layout atual: escala pela proporção e espelha em x quando canhoto.
static func offset_for(base: Vector2, settings: Dictionary) -> Vector2:
	var o := base * button_scale(settings)
	if is_left_handed(settings):
		o.x = -o.x
	return o
