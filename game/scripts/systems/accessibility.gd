class_name Accessibility
extends RefCounted
## Acessibilidade (§3.6): lê as preferências de settings do save e as traduz em
## valores que os sistemas aplicam. Lógica pura — sem UI, sem estado próprio.
##
## - reduce_shake: zera o tremor de câmera (conforto, enjoo de movimento).
## - large_text: aumenta a fonte padrão dos menus/diálogo.

const LARGE_FONT_SIZE := 22
const BASE_FONT_SIZE := 16

static func is_reduce_shake(settings: Dictionary) -> bool:
	return bool(settings.get("reduce_shake", false))

static func is_large_text(settings: Dictionary) -> bool:
	return bool(settings.get("large_text", false))

## Multiplicador do tremor de câmera: 0 quando reduzido, 1 caso contrário.
static func shake_mult(settings: Dictionary) -> float:
	return 0.0 if is_reduce_shake(settings) else 1.0

## Tamanho de fonte padrão a aplicar no tema raiz.
static func font_size(settings: Dictionary) -> int:
	return LARGE_FONT_SIZE if is_large_text(settings) else BASE_FONT_SIZE

## Constrói (ou reusa) um tema com o tamanho de fonte da preferência e o instala
## na raiz da árvore, escalando todos os Controls que não fixam a própria fonte.
static func apply_to_tree(tree: SceneTree, settings: Dictionary) -> void:
	if tree == null or tree.root == null:
		return
	var theme: Theme = tree.root.theme
	if theme == null:
		theme = Theme.new()
		tree.root.theme = theme
	theme.default_font_size = font_size(settings)
