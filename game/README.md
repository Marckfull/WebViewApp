# Ecos de Lirael — Protótipo de Combate (Fase 0)

Projeto **Godot 4.3+** do action-RPG descrito em [`../docs/GDD-PLANO-DO-JOGO.md`](../docs/GDD-PLANO-DO-JOGO.md).

Esta é a **Fase 0** do plano: uma arena de treino ("gym") em greybox que valida o
núcleo do combate souls-like antes de investirmos em arte, história e conteúdo.
O objetivo desta fase é responder uma pergunta: **o combate é divertido?**

## O que já está jogável

- **Movimentação** analógica em 8 direções (Aria, o retângulo verde-água 🙂).
- **Ataque** com varredura de espada, custo de stamina e janela de acerto.
- **Esquiva-rolamento** com i-frames (invencibilidade durante o rolamento).
- **Stamina** com regeneração após pausa curta (o coração do souls-like).
- **Inimigos "Ecoados"**: perseguem, telegrafam o golpe (piscam) e investem;
  janela de punição após o ataque.
- **Mini-boss "Brutamontes"**: mais lento, mais dano, resiste a atordoamento
  (poise) — o proto-boss da Fase 0.
- **Loop de morte souls-like**: ao morrer você perde seus **Ecos** e eles ficam
  no local da morte; volte lá para recuperá-los (um novo drop substitui o antigo).
- **Santuário** (bonfire): descansar cura, recarrega frascos/stamina e **repõe
  todos os inimigos**.
- **HUD**: vida, stamina, contador de Ecos e mensagens de evento.
- **Controles de toque**: joystick virtual flutuante + botões multi-touch
  (aparecem só em dispositivos com touchscreen).

## Como rodar

1. Instale o [Godot 4.3+](https://godotengine.org/download) (a versão padrão, não a .NET).
2. Abra o Godot → *Import* → selecione `game/project.godot`.
3. Pressione **F5** para rodar.

### Controles (desktop)

| Ação | Tecla | Gamepad |
|---|---|---|
| Mover | WASD / setas | Analógico esquerdo |
| Atacar | J ou Z | X / Quadrado |
| Rolar | K ou Espaço | A / Cruz |
| Interagir (descansar) | E ou Enter | Y / Triângulo |

No Android/touchscreen: arraste na metade esquerda da tela para mover;
botões ATACAR / ROLAR / USAR à direita.

## Exportar para Android

1. Godot → *Editor* → *Manage Export Templates* → baixe os templates da sua versão.
2. Instale o Android SDK (ou use o Android Studio já instalado) e configure o
   caminho em *Editor Settings* → *Export* → *Android*.
3. *Project* → *Export* → *Add* → *Android*; gere uma debug keystore quando pedido.
4. Exporte o APK e instale no aparelho (`adb install`).

O projeto já está configurado para mobile: renderer `gl_compatibility`,
resolução interna 640×360 com stretch `viewport/expand`, orientação
landscape por sensor e filtro de textura *nearest* (pixel art).

## Estrutura

```
game/
├── core/        # autoloads: estado global (Ecos) e event bus
├── systems/     # componentes reutilizáveis: Health, Stamina, Hitbox, Hurtbox
├── entities/
│   ├── player/  # Aria: máquina de estados (mover/rolar/atacar/dano/morte)
│   └── enemies/ # EnemyBase + variantes (Ecoado, Brutamontes)
├── world/       # arena gym, santuário, spawners, pickups de Eco
└── ui/          # HUD, joystick virtual, botões de toque
```

## Próximos passos (ver GDD, seção 8)

1. Playtest do combate e ajuste fino dos números (velocidades, custos, janelas).
2. Lock-on de alvo (Z-targeting adaptado a touch).
3. Boss real com 2 fases para fechar o gate da Fase 0.
4. Início da vertical slice: vila de Pedra-Alva + primeira dungeon.
