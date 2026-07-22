class_name Shop
extends RefCounted
## Lógica pura da loja do mercador (§3.5). Sem UI/estado: só a aritmética da
## compra em Ecos. O que se compra (e de qual bolsa) vem de um ShopData.

## Valor de revenda de um recurso/consumível (§3.5). Sempre menor que o de compra.
const SELL_VALUES := {
	"pocao_cura": 15,
	"pocao_vigor": 12,
	"erva": 6,
	"minerio": 10,
	"minerio_ressonante": 20,
	"minerio_do_eco": 35,
}

static func sell_value(id: StringName) -> int:
	return int(SELL_VALUES.get(String(id), 3))

static func can_afford(price: int, ecos: int) -> bool:
	return price >= 0 and ecos >= price

## Tenta pagar `price` Ecos. Retorna {ok, ecos} sem mutar os argumentos.
static func try_buy(price: int, ecos: int) -> Dictionary:
	if not can_afford(price, ecos):
		return {"ok": false, "ecos": ecos}
	return {"ok": true, "ecos": ecos - price}
