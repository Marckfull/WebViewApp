class_name ItemNames
extends RefCounted
## Nomes amigáveis de consumíveis/recursos/itens-chave (§3.5) para a UI. Lógica
## pura: id -> rótulo, com fallback capitalizado. Armas/equipamento têm display_name
## no próprio recurso; aqui ficam os que são só ids na bolsa.

const NAMES := {
	"pocao_cura": "Poção de Cura",
	"pocao_vigor": "Poção de Vigor",
	"minerio": "Minério bruto",
	"minerio_ressonante": "Minério ressonante",
	"minerio_do_eco": "Minério do eco",
	"erva": "Erva",
	"erva_prateada": "Erva prateada",
	"erva_do_eco": "Erva do eco",
	"madeira": "Madeira",
	"cogumelo": "Cogumelo",
	"peixe": "Peixe",
	"inseto": "Inseto",
	"elixir_do_eco": "Elixir do Eco",
	"refeicao": "Refeição Farta",
	"gancho": "Gancho-corda",
	"bomba": "Bomba de Eco",
	"lente": "Lente da Verdade",
	"botas": "Botas de Corrente",
}

static func label(id: StringName) -> String:
	var key := String(id)
	return NAMES.get(key, key.capitalize())
