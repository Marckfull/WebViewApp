package com.chuvadeletras.game.domain.words

import com.chuvadeletras.game.domain.model.WordClue

/** Temas de palavras. Cada partida sorteia um tema (ou usa "Geral"). */
enum class WordTheme(val label: String, val emoji: String) {
    GERAL("Geral", "🎲"),
    ANIMAIS("Animais", "🦜"),
    COMIDA("Comida", "🍲"),
    BRASIL("Brasil", "🇧🇷"),
    TECNOLOGIA("Tecnologia", "💻"),
    NATUREZA("Natureza", "🌳"),
    ESPORTE("Esporte", "⚽")
}

/**
 * Banco de palavras do jogo.
 *
 * Regra de ouro: toda resposta é normalizada para A-Z maiúsculo, sem acento e
 * sem cedilha, porque a bandeja só distribui letras desse alfabeto.
 */
object WordBank {

    fun forTheme(theme: WordTheme): List<WordClue> = when (theme) {
        WordTheme.GERAL -> (animais + comida + brasil + tecnologia + natureza + esporte + gerais)
        WordTheme.ANIMAIS -> animais + gerais.take(10)
        WordTheme.COMIDA -> comida + gerais.take(10)
        WordTheme.BRASIL -> brasil + gerais.take(10)
        WordTheme.TECNOLOGIA -> tecnologia + gerais.take(10)
        WordTheme.NATUREZA -> natureza + gerais.take(10)
        WordTheme.ESPORTE -> esporte + gerais.take(10)
    }.map { it.normalized() }.distinctBy { it.answer }

    /** Palavras filtradas por tamanho máximo, para caber na dificuldade escolhida. */
    fun forTheme(theme: WordTheme, maxLength: Int): List<WordClue> =
        forTheme(theme).filter { it.answer.length in 3..maxLength }

    private fun WordClue.normalized(): WordClue = copy(answer = normalize(answer))

    /** Tira acento, cedilha e qualquer caractere que não seja letra. */
    fun normalize(raw: String): String = buildString {
        raw.uppercase().forEach { c ->
            val mapped = when (c) {
                'Á', 'À', 'Â', 'Ã', 'Ä' -> 'A'
                'É', 'È', 'Ê', 'Ë' -> 'E'
                'Í', 'Ì', 'Î', 'Ï' -> 'I'
                'Ó', 'Ò', 'Ô', 'Õ', 'Ö' -> 'O'
                'Ú', 'Ù', 'Û', 'Ü' -> 'U'
                'Ç' -> 'C'
                'Ñ' -> 'N'
                else -> c
            }
            if (mapped in 'A'..'Z') append(mapped)
        }
    }

    private val animais = listOf(
        WordClue("ONCA", "Maior felino das Américas, símbolo do Pantanal"),
        WordClue("TATU", "Bicho que se enrola em bola e usa armadura"),
        WordClue("ARARA", "Ave azul ou vermelha de bico curvo e grito forte"),
        WordClue("TUCANO", "Ave famosa pelo bico enorme e colorido"),
        WordClue("PREGUICA", "Mamífero que se move devagar e vive de cabeça para baixo"),
        WordClue("BALEIA", "Maior animal do planeta, vive no mar"),
        WordClue("GOLFINHO", "Mamífero marinho brincalhão e muito inteligente"),
        WordClue("TAMANDUA", "Come formigas com a língua comprida"),
        WordClue("CAPIVARA", "Maior roedor do mundo, adora água"),
        WordClue("JACARE", "Réptil de mandíbula forte dos rios brasileiros"),
        WordClue("COBRA", "Réptil sem pernas que rasteja"),
        WordClue("CAVALO", "Animal de sela e de corrida"),
        WordClue("GATO", "Bicho de estimação que mia"),
        WordClue("PATO", "Ave que faz quá-quá e nada na lagoa"),
        WordClue("ABELHA", "Inseto que produz mel"),
        WordClue("FORMIGA", "Inseto trabalhador que vive em colônia"),
        WordClue("TARTARUGA", "Réptil que carrega o próprio casco"),
        WordClue("PANTERA", "Felino de pelagem escura"),
        WordClue("LOBO", "Ancestral selvagem do cachorro"),
        WordClue("RAPOSA", "Bicho esperto das fábulas"),
        WordClue("PINGUIM", "Ave que não voa mas nada muito bem"),
        WordClue("MACACO", "Primata que adora banana"),
        WordClue("ELEFANTE", "Tem tromba e as maiores orelhas da savana"),
        WordClue("GIRAFA", "Tem o pescoço mais comprido do mundo"),
        WordClue("ZEBRA", "Cavalo listrado da África"),
        WordClue("TIGRE", "Felino laranja de listras pretas"),
        WordClue("CORUJA", "Ave noturna de olhos grandes"),
        WordClue("SAPO", "Anfíbio que coaxa e pula"),
        WordClue("PEIXE", "Vive na água e respira por guelras"),
        WordClue("CAMELO", "Atravessa o deserto com corcovas")
    )

    private val comida = listOf(
        WordClue("FEIJOADA", "Prato brasileiro de feijão preto com carnes"),
        WordClue("BRIGADEIRO", "Doce de chocolate com granulado"),
        WordClue("PAODEQUEIJO", "Quitute mineiro assado e recheado de queijo"),
        WordClue("TAPIOCA", "Disco branco de goma feito na chapa"),
        WordClue("ACARAJE", "Bolinho baiano frito no dendê"),
        WordClue("COXINHA", "Salgado em formato de gota com frango"),
        WordClue("CUSCUZ", "Prato de milho cozido no vapor"),
        WordClue("ARROZ", "Acompanha o feijão no prato do dia a dia"),
        WordClue("FAROFA", "Mandioca torrada que acompanha o churrasco"),
        WordClue("PUDIM", "Doce de leite condensado com calda de caramelo"),
        WordClue("CAFE", "Bebida quente que desperta o Brasil"),
        WordClue("QUEIJO", "Derivado do leite, bom na pizza"),
        WordClue("MANGA", "Fruta tropical amarela e suculenta"),
        WordClue("ABACAXI", "Fruta de coroa espinhosa"),
        WordClue("BANANA", "Fruta amarela e curva"),
        WordClue("MORANGO", "Fruta vermelha de sementinhas por fora"),
        WordClue("LARANJA", "Fruta cítrica do suco da manhã"),
        WordClue("MELANCIA", "Fruta verde por fora e vermelha por dentro"),
        WordClue("BOLO", "Doce de forno para aniversário"),
        WordClue("SORVETE", "Sobremesa gelada de casquinha"),
        WordClue("PIZZA", "Disco de massa com molho e queijo"),
        WordClue("MACARRAO", "Massa italiana de molho vermelho"),
        WordClue("SALADA", "Mistura de verduras cruas"),
        WordClue("CHOCOLATE", "Feito de cacau, derrete na boca"),
        WordClue("PIPOCA", "Estoura na panela e vai pro cinema"),
        WordClue("SUCO", "Bebida feita da fruta espremida"),
        WordClue("PASTEL", "Frito na feira, de queijo ou carne"),
        WordClue("VATAPA", "Creme baiano de pão, camarão e dendê"),
        WordClue("MOQUECA", "Ensopado de peixe no leite de coco"),
        WordClue("CANJICA", "Doce de milho branco da festa junina")
    )

    private val brasil = listOf(
        WordClue("SAMBA", "Ritmo nascido no Rio de Janeiro"),
        WordClue("CARNAVAL", "Maior festa popular do país"),
        WordClue("AMAZONIA", "Maior floresta tropical do mundo"),
        WordClue("PANTANAL", "Maior planície alagável do planeta"),
        WordClue("BAHIA", "Estado do acarajé e do axé"),
        WordClue("CEARA", "Estado do humor e das dunas de Jericoacoara"),
        WordClue("PARANA", "Estado das Cataratas do Iguaçu"),
        WordClue("BRASILIA", "Capital projetada por Niemeyer"),
        WordClue("SALVADOR", "Primeira capital do Brasil"),
        WordClue("MARACANA", "Templo do futebol carioca"),
        WordClue("CAPOEIRA", "Arte que mistura luta, dança e música"),
        WordClue("FORRO", "Ritmo de sanfona, zabumba e triângulo"),
        WordClue("SERTAO", "Região seca do interior nordestino"),
        WordClue("CERRADO", "Savana brasileira de árvores retorcidas"),
        WordClue("CAATINGA", "Bioma do Nordeste, seco e espinhoso"),
        WordClue("BOSSANOVA", "Gênero suave de João Gilberto e Tom Jobim"),
        WordClue("CHIMARRAO", "Bebida de erva-mate do Sul"),
        WordClue("FREVO", "Dança rápida do carnaval de Recife"),
        WordClue("MARANHAO", "Estado dos Lençóis e do bumba meu boi"),
        WordClue("IPIRANGA", "Riacho onde foi proclamada a Independência"),
        WordClue("CAJU", "Fruta do Nordeste com castanha pendurada"),
        WordClue("BUMBA", "Início do nome do boi festejado no Norte"),
        WordClue("SANFONA", "Instrumento do forró, também chamado acordeom"),
        WordClue("BERIMBAU", "Arco musical que comanda a roda de capoeira"),
        WordClue("CANGACO", "Movimento de bandoleiros de Lampião"),
        WordClue("JANGADA", "Barco a vela típico das praias nordestinas"),
        WordClue("CUIABA", "Capital do Mato Grosso"),
        WordClue("RECIFE", "Capital pernambucana cortada por rios"),
        WordClue("MANAUS", "Capital do Amazonas, do Teatro Amazonas"),
        WordClue("PELE", "Rei do futebol, camisa 10 eterna")
    )

    private val tecnologia = listOf(
        WordClue("TECLADO", "Onde ficam as teclas do computador"),
        WordClue("MOUSE", "Aponta e clica na tela"),
        WordClue("MONITOR", "Tela do computador"),
        WordClue("INTERNET", "Rede mundial de computadores"),
        WordClue("SENHA", "Segredo que protege sua conta"),
        WordClue("ARQUIVO", "Documento salvo no computador"),
        WordClue("PASTA", "Guarda vários arquivos juntos"),
        WordClue("NUVEM", "Onde ficam os arquivos online"),
        WordClue("ROBO", "Máquina que executa tarefas sozinha"),
        WordClue("CODIGO", "O que o programador escreve"),
        WordClue("APLICATIVO", "Programa instalado no celular"),
        WordClue("BATERIA", "Guarda a energia do celular"),
        WordClue("CAMERA", "Captura fotos e vídeos"),
        WordClue("PIXEL", "Menor pontinho de uma imagem digital"),
        WordClue("SATELITE", "Orbita a Terra e leva sinal de TV"),
        WordClue("DRONE", "Aeronave pequena controlada à distância"),
        WordClue("VIRUS", "Programa malicioso que infecta o sistema"),
        WordClue("BACKUP", "Cópia de segurança dos seus dados"),
        WordClue("SERVIDOR", "Computador que atende outros pela rede"),
        WordClue("MEMORIA", "Onde o computador guarda informação"),
        WordClue("BLUETOOTH", "Conexão sem fio de curta distância"),
        WordClue("EMAIL", "Correio eletrônico"),
        WordClue("SITE", "Endereço que se abre no navegador"),
        WordClue("REDE", "Conjunto de máquinas conectadas"),
        WordClue("CHIP", "Pequena peça de silício do celular"),
        WordClue("FONE", "Ouve música sem incomodar ninguém"),
        WordClue("TABLET", "Tela grande sem teclado físico"),
        WordClue("NAVEGADOR", "Programa que abre páginas da web"),
        WordClue("ALGORITMO", "Receita de passos que o programa segue"),
        WordClue("DADOS", "Informação que o sistema processa")
    )

    private val natureza = listOf(
        WordClue("CHUVA", "Cai do céu e molha tudo"),
        WordClue("TROVAO", "Barulho que vem depois do relâmpago"),
        WordClue("RELAMPAGO", "Risco de luz no céu da tempestade"),
        WordClue("NUVEM", "Algodão branco que passeia no céu"),
        WordClue("ARCOIRIS", "Aparece colorido depois da chuva"),
        WordClue("VENTO", "Ar em movimento"),
        WordClue("RIO", "Curso de água que corre para o mar"),
        WordClue("MAR", "Água salgada que banha a praia"),
        WordClue("MONTANHA", "Elevação bem alta do terreno"),
        WordClue("FLORESTA", "Muitas árvores juntas"),
        WordClue("DESERTO", "Lugar de areia e quase nenhuma chuva"),
        WordClue("VULCAO", "Montanha que cospe lava"),
        WordClue("TERREMOTO", "Tremor forte do solo"),
        WordClue("NEVE", "Chuva congelada e branquinha"),
        WordClue("SOL", "Estrela que ilumina o nosso dia"),
        WordClue("LUA", "Satélite natural da Terra"),
        WordClue("ESTRELA", "Ponto brilhante no céu da noite"),
        WordClue("PLANETA", "A Terra é um deles"),
        WordClue("SEMENTE", "Dela nasce a planta"),
        WordClue("RAIZ", "Parte da planta que fica enterrada"),
        WordClue("FOLHA", "Parte verde da planta"),
        WordClue("FLOR", "Parte perfumada e colorida da planta"),
        WordClue("AREIA", "Cobre a praia e escapa pelos dedos"),
        WordClue("PEDRA", "Dura, pesada e não flutua"),
        WordClue("CACHOEIRA", "Água que despenca de um paredão"),
        WordClue("ORVALHO", "Gotinhas na grama de manhã cedo"),
        WordClue("NEBLINA", "Névoa baixa que atrapalha a visão"),
        WordClue("GRANIZO", "Pedrinhas de gelo que caem na tempestade"),
        WordClue("MARE", "Sobe e desce puxada pela Lua"),
        WordClue("POCA", "Ficou no chão depois que a chuva parou")
    )

    private val esporte = listOf(
        WordClue("FUTEBOL", "Esporte mais popular do Brasil"),
        WordClue("VOLEI", "Time passa a bola por cima da rede"),
        WordClue("BASQUETE", "Encesta a bola na cesta alta"),
        WordClue("NATACAO", "Esporte de piscina e de nado"),
        WordClue("CORRIDA", "Vence quem chega primeiro"),
        WordClue("SURFE", "Esporte de prancha nas ondas"),
        WordClue("TENIS", "Raquete, rede e bolinha amarela"),
        WordClue("BOXE", "Luta de luvas no ringue"),
        WordClue("JUDO", "Arte marcial japonesa de quimono"),
        WordClue("SKATE", "Prancha com quatro rodinhas"),
        WordClue("GOLEIRO", "Único que pode usar as mãos no futebol"),
        WordClue("ARBITRO", "Apita e aplica as regras do jogo"),
        WordClue("MEDALHA", "Prêmio de ouro, prata ou bronze"),
        WordClue("OLIMPIADA", "Maior evento esportivo do mundo"),
        WordClue("TORCIDA", "Grita e canta na arquibancada"),
        WordClue("ESTADIO", "Lugar onde acontece a partida"),
        WordClue("PENALTI", "Cobrança da marca da cal"),
        WordClue("ESCANTEIO", "Cobrança feita da quina do campo"),
        WordClue("ATLETA", "Quem pratica esporte de alto nível"),
        WordClue("TREINO", "Preparação antes da competição"),
        WordClue("CAMPEAO", "Quem levanta a taça no fim"),
        WordClue("RAQUETE", "Instrumento do tênis e do badminton"),
        WordClue("CICLISMO", "Esporte sobre duas rodas e pedais"),
        WordClue("XADREZ", "Jogo de tabuleiro de reis e peões"),
        WordClue("GINASTICA", "Esporte de saltos, giros e equilíbrio"),
        WordClue("REMO", "Esporte de barco movido a braço"),
        WordClue("GOL", "Objetivo de todo atacante"),
        WordClue("CESTA", "Vale dois ou três pontos no basquete"),
        WordClue("PISTA", "Onde acontece a corrida de atletismo"),
        WordClue("TATAME", "Piso das lutas de quimono")
    )

    private val gerais = listOf(
        WordClue("LIVRO", "Feito de páginas e histórias"),
        WordClue("ESCOLA", "Lugar de estudar e aprender"),
        WordClue("AMIGO", "Companheiro de toda hora"),
        WordClue("MUSICA", "Arte feita de sons e ritmo"),
        WordClue("CIDADE", "Lugar de muita gente e muitas ruas"),
        WordClue("JANELA", "Abertura que deixa entrar luz e ar"),
        WordClue("PORTA", "Abre e fecha a entrada"),
        WordClue("RELOGIO", "Marca as horas"),
        WordClue("ESPELHO", "Devolve a sua imagem"),
        WordClue("CHAVE", "Abre a fechadura"),
        WordClue("CADEIRA", "Móvel de sentar"),
        WordClue("MESA", "Móvel onde a família se reúne"),
        WordClue("SONHO", "Acontece enquanto você dorme"),
        WordClue("VIAGEM", "Deslocamento até um lugar novo"),
        WordClue("FESTA", "Comemoração com música e gente"),
        WordClue("CARTA", "Mensagem escrita enviada pelo correio"),
        WordClue("PALAVRA", "Peça básica de toda frase"),
        WordClue("LETRA", "Cada símbolo do alfabeto"),
        WordClue("JOGO", "Diversão com regras e vencedor"),
        WordClue("PREMIO", "Recompensa de quem se destaca"),
        WordClue("SEGREDO", "Aquilo que não se conta"),
        WordClue("CORAGEM", "Enfrenta o medo assim mesmo"),
        WordClue("SORRISO", "Aparece quando a gente fica feliz"),
        WordClue("ESTRADA", "Caminho longo entre cidades"),
        WordClue("PONTE", "Liga as duas margens")
    )
}
