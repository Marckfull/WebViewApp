class_name Endings
extends RefCounted
## Lógica pura dos finais do Ato 3 (§2). Sem UI/estado: só quais finais estão
## disponíveis (o secreto exige as 12 Memórias) e seus textos.

const SILENCIAR := &"silenciar"
const COMPLETAR := &"completar"
const SECRETO := &"secreto"

const TOTAL_MEMORIES := 12
## Quantas vezes Aria precisa tocar a Canção do Mundo na fase 2 (duelo de melodias).
const DUEL_HARMONY_NEEDED := 3

static func secret_available(memory_count: int) -> bool:
	return memory_count >= TOTAL_MEMORIES

## Finais disponíveis dado o nº de Memórias coletadas. Os dois primeiros sempre;
## o secreto só com as 12.
static func available(memory_count: int) -> Array:
	var out: Array = [SILENCIAR, COMPLETAR]
	if secret_available(memory_count):
		out.append(SECRETO)
	return out

static func title(ending: StringName) -> String:
	match ending:
		SILENCIAR: return "Silenciar"
		COMPLETAR: return "Completar"
		SECRETO: return "Lembrar em Coro"
		_: return ""

static func epilogue(ending: StringName) -> String:
	match ending:
		SILENCIAR:
			return "Aria desfaz Selene nota a nota. O Silêncio para — mas a dor dela, e o nome que ela guardava, somem para sempre. Lys volta. O mundo cura, incompleto."
		COMPLETAR:
			return "Aria canta a canção de Selene até o fim, atravessando a dor com ela. Selene descansa; o Silêncio se desfaz por dentro. O eco fica com Aria agora."
		SECRETO:
			return "Aria e Lys cantam juntas a Canção completa. As cinco Guardiãs e Selene voltam por um instante como som — e o mundo lembra de todos os nomes, inclusive o que fora esquecido. Nem apagar, nem carregar sozinha: lembrar em coro."
		_: return ""
