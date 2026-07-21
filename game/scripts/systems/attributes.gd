class_name Attributes
extends RefCounted
## Lógica pura de atributos e progressão (§3.3). Sem estado/UI — testável (§6.3).
## Aria gasta Ecos nos santuários para subir atributos; o custo escala com o
## nível total (estilo souls: cada ponto custa mais que o anterior).

const KEYS := ["vitalidade", "stamina", "forca", "destreza", "harmonia"]

const BASE_HP := 100.0
const HP_PER_VIT := 20.0
const BASE_STAMINA := 100.0
const STAMINA_PER_STA := 12.0
const BASE_COST := 25
const COST_PER_LEVEL := 15

## Efeitos de combate por atributo (§3.3). Cada ponto acima de 1 melhora um eixo:
## Força = dano corpo-a-corpo, Destreza = custo de stamina das ações (agilidade),
## Harmonia = janela de parry (sintonia com o ritmo do mundo).
const DAMAGE_PER_FORCA := 0.08
const STAMINA_DISCOUNT_PER_DEX := 0.05
const STAMINA_DISCOUNT_FLOOR := 0.5   ## no máximo -50% de custo
const PARRY_BONUS_PER_HARMONIA := 0.06

## Custo em Ecos do PRÓXIMO ponto, dado o nível total já investido.
static func cost_for_total_level(total_level: int) -> int:
	return BASE_COST + COST_PER_LEVEL * maxi(total_level, 0)

## Soma dos níveis de todos os atributos (cada um começa em 1).
static func total_level(attrs: Dictionary) -> int:
	var sum := 0
	for k in KEYS:
		sum += int(attrs.get(k, 1))
	return sum

static func max_hp_for(vitalidade: int) -> float:
	return BASE_HP + HP_PER_VIT * (vitalidade - 1)

static func max_stamina_for(stamina_attr: int) -> float:
	return BASE_STAMINA + STAMINA_PER_STA * (stamina_attr - 1)

## Multiplicador de dano corpo-a-corpo pela Força.
static func damage_mult_for(forca: int) -> float:
	return 1.0 + DAMAGE_PER_FORCA * (maxi(forca, 1) - 1)

## Multiplicador do custo de stamina pela Destreza (menor = mais barato).
static func stamina_cost_mult_for(destreza: int) -> float:
	return maxf(STAMINA_DISCOUNT_FLOOR, 1.0 - STAMINA_DISCOUNT_PER_DEX * (maxi(destreza, 1) - 1))

## Multiplicador da janela de parry pela Harmonia (maior = mais tolerante).
static func parry_window_mult_for(harmonia: int) -> float:
	return 1.0 + PARRY_BONUS_PER_HARMONIA * (maxi(harmonia, 1) - 1)

## Tenta gastar Ecos para subir um atributo. Retorna o dicionário atualizado
## {ok, ecos, attrs}. Não muta os argumentos.
static func try_level_up(attr_key: String, attrs: Dictionary, ecos: int) -> Dictionary:
	var cost := cost_for_total_level(total_level(attrs))
	if not KEYS.has(attr_key) or ecos < cost:
		return {"ok": false, "ecos": ecos, "attrs": attrs.duplicate(true)}
	var new_attrs := attrs.duplicate(true)
	new_attrs[attr_key] = int(new_attrs.get(attr_key, 1)) + 1
	return {"ok": true, "ecos": ecos - cost, "attrs": new_attrs}
