# assets/

Pasta de assets importados (sprites, tilesets, áudio). **Vazia de propósito** no
commit inicial — o greybox usa formas geométricas (Polygon2D) para não depender de
arte antes da hora.

## Como popular (ver `/docs/ECOS_DE_LIRAEL_recursos_gratuitos.md`)

Sugestão de organização por pack, com a licença ao lado:

```
assets/
  ninja_adventure/     # CC0 — pixel-boy (greybox -> vertical slice)
    LICENSE.txt
  dungeon_crawl/       # CC0 — OpenGameArt (tiles de dungeon)
    LICENSE.txt
  kenney_ui/           # CC0 — Kenney (ícones, UI)
    LICENSE.txt
  audio_sfx/           # CC0 — Kenney Audio / OpenGameArt
    LICENSE.txt
```

**Regra obrigatória:** todo asset entra junto com seu `LICENSE.txt` e um registro
em `/game/CREDITS.md` no mesmo commit. Ver seção 9 (Governança de Licenças) do catálogo.
