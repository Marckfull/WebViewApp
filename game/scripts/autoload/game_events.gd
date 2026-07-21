extends Node
## GameEvents — barramento global de sinais (autoload).
##
## Desacopla sistemas (combate, UI, save, bestiário) via sinais, seguindo a
## arquitetura data-driven do §6.2. Quem dispara não precisa conhecer quem ouve.

## Combate
signal player_died(death_position: Vector2, ecos_dropped: int)
signal enemy_defeated(enemy_id: StringName, position: Vector2)
signal poise_broken(target: Node)         ## postura quebrada -> finalização (§3.2)
signal parry_success(target: Node)        ## parry no tempo certo -> crítico (§3.2)
signal damage_dealt(target: Node, amount: float)

## Boss (§3.2 — 2 fases, leitura de padrões)
signal boss_spawned(boss_name: String, max_hp: float)
signal boss_health_changed(current: float, maximum: float)
signal boss_phase_changed(phase: int)
signal boss_defeated(boss_id: StringName)

## Recursos e progressão
signal ecos_changed(total: int)           ## moeda de XP/compra (§3.2)
signal stamina_changed(current: float, maximum: float)
signal health_changed(current: float, maximum: float)
signal flasks_changed(current: int, maximum: int)  ## Frascos de Essência (§3.2)
signal weapon_changed(display_name: String)        ## arma equipada (§3.3)
signal consumable_changed(id: StringName, count: int)  ## poções etc. (§3.5)
signal consumable_slot_selected(index: int)            ## slot ativo no HUD (§3.5)

## Diálogo (§3.5 lore, NPCs)
signal dialogue_started
signal dialogue_finished(dialogue_id: StringName)

## Mundo
signal rested_at_shrine(shrine_id: StringName)   ## santuário/bonfire (§3.2)
signal melody_played(melody_id: StringName)      ## ocarina (§3.1)
signal day_time_changed(value: float)            ## ciclo dia/noite (§3.5, §4)
signal area_discovered(area_id: StringName)      ## mapa que se desenha (§3.5)
signal item_obtained(item_id: StringName)        ## item-chave de dungeon (§3.3)
signal bestiary_entry_unlocked(enemy_id: StringName)

## Meta
signal game_saved(slot: int)
signal game_loaded(slot: int)
