class_name Shop
extends RefCounted
## Lógica pura da loja do mercador (§3.5). Sem UI/estado: só a aritmética da
## compra em Ecos. O que se compra (e de qual bolsa) vem de um ShopData.

static func can_afford(price: int, ecos: int) -> bool:
	return price >= 0 and ecos >= price

## Tenta pagar `price` Ecos. Retorna {ok, ecos} sem mutar os argumentos.
static func try_buy(price: int, ecos: int) -> Dictionary:
	if not can_afford(price, ecos):
		return {"ok": false, "ecos": ecos}
	return {"ok": true, "ecos": ecos - price}
