class_name WorldMap
extends RefCounted
## Mapa-múndi (§3.5): traduz os caminhos de cena em nome + região e cruza com o
## fog-of-war persistido (world.map) para saber o que Aria já descobriu. Lógica
## pura — a tela de mapa da pausa desenha a partir daqui.

## Ordem das regiões na tela (do prólogo ao endgame).
const REGIONS: Array = [
	"Pedra-Alva", "Cripta das Guardiãs", "Encruzilhada",
	"Floresta Sussurrante", "Forja Afundada", "Torre dos Ventos",
	"Necrópole de Sal", "Coração Mudo",
]

## scene_file_path -> { nome curto, região }.
const ROOMS := {
	"res://scenes/world/vila_pedra_alva.tscn": {"name": "Vila", "region": "Pedra-Alva"},
	"res://scenes/world/cripta_das_guardias.tscn": {"name": "Salão das Guardiãs", "region": "Cripta das Guardiãs"},
	"res://scenes/world/cripta_ala2.tscn": {"name": "Ala Profunda", "region": "Cripta das Guardiãs"},
	"res://scenes/world/encruzilhada.tscn": {"name": "Encruzilhada", "region": "Encruzilhada"},
	"res://scenes/world/floresta_atrio.tscn": {"name": "Átrio", "region": "Floresta Sussurrante"},
	"res://scenes/world/floresta_bosque.tscn": {"name": "Bosque", "region": "Floresta Sussurrante"},
	"res://scenes/world/floresta_sussurrante.tscn": {"name": "Mata Fechada", "region": "Floresta Sussurrante"},
	"res://scenes/world/forja_atrio.tscn": {"name": "Átrio", "region": "Forja Afundada"},
	"res://scenes/world/forja_fornalhas.tscn": {"name": "Fornalhas", "region": "Forja Afundada"},
	"res://scenes/world/forja_afundada.tscn": {"name": "Coração da Forja", "region": "Forja Afundada"},
	"res://scenes/world/torre_atrio.tscn": {"name": "Átrio", "region": "Torre dos Ventos"},
	"res://scenes/world/torre_ventos_altos.tscn": {"name": "Ventos Altos", "region": "Torre dos Ventos"},
	"res://scenes/world/torre_dos_ventos.tscn": {"name": "Cúpula", "region": "Torre dos Ventos"},
	"res://scenes/world/necropole_atrio.tscn": {"name": "Átrio", "region": "Necrópole de Sal"},
	"res://scenes/world/necropole_tumbas.tscn": {"name": "Tumbas de Sal", "region": "Necrópole de Sal"},
	"res://scenes/world/necropole_de_sal.tscn": {"name": "Salinas", "region": "Necrópole de Sal"},
	"res://scenes/world/arena_ecos.tscn": {"name": "Arena dos Ecos", "region": "Coração Mudo"},
	"res://scenes/world/coracao_mudo.tscn": {"name": "Coração Mudo", "region": "Coração Mudo"},
}

static func name_of(path: String) -> String:
	return ROOMS.get(path, {}).get("name", "")

static func region_of(path: String) -> String:
	return ROOMS.get(path, {}).get("region", "")

## Uma sala conta como descoberta se tem células reveladas no fog-of-war.
static func is_discovered(path: String, world_state: Dictionary) -> bool:
	var maps: Dictionary = world_state.get("map", {})
	var cells: Variant = maps.get(path, [])
	return cells is Array and (cells as Array).size() > 0

## Salas descobertas de uma região, na ordem de ROOMS.
static func discovered_in(region: String, world_state: Dictionary) -> Array:
	var out: Array = []
	for path in ROOMS:
		if ROOMS[path]["region"] == region and is_discovered(path, world_state):
			out.append(path)
	return out

## Conta total de salas descobertas (para "N/M explorado").
static func discovered_count(world_state: Dictionary) -> int:
	var n := 0
	for path in ROOMS:
		if is_discovered(path, world_state):
			n += 1
	return n

static func total_rooms() -> int:
	return ROOMS.size()
