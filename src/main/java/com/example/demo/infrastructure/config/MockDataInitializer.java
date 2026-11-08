package com.example.demo.infrastructure.config;

import com.example.demo.controller.dtos.request.*;
import com.example.demo.controller.dtos.response.ClienteResponseDTO;
import com.example.demo.controller.dtos.response.PedidoResponseDTO;
import com.example.demo.controller.dtos.response.ProdutoResponseDTO;
import com.example.demo.controller.dtos.response.VariacaoResponseDTO;
import com.example.demo.infrastructure.entity.enums.PedidoStatus;
import com.example.demo.infrastructure.repository.UsuarioRepository;
import com.example.demo.service.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

// Popula o banco com dados de demonstração (lojas, clientes, produtos com fotos e variações, pedidos e avaliações)
// para ver o site funcionando. Roda uma única vez: se a loja de demonstração já existir, não faz nada.
// Para desligar: MOCK_DATA=false (ou app.mock.enabled=false no application.properties)
@Component
@Order(2) // depois do AdminInitializer
@ConditionalOnProperty(name = "app.mock.enabled", havingValue = "true")
public class MockDataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(MockDataInitializer.class);

    public static final String SENHA = "senha123";
    public static final String VENDEDOR_TECH = "tech@kamistore.com";
    public static final String VENDEDOR_MODA = "moda@kamistore.com";
    public static final String CLIENTE_ANA = "ana@kamistore.com";
    public static final String CLIENTE_BRUNO = "bruno@kamistore.com";

    private final UsuarioRepository usuarioRepository;
    private final VendedorService vendedorService;
    private final ClienteService clienteService;
    private final ProdutoService produtoService;
    private final CarrinhoService carrinhoService;
    private final PedidoService pedidoService;
    private final AvaliacaoService avaliacaoService;

    private final TransactionTemplate transactionTemplate;

    @PersistenceContext
    private EntityManager entityManager;

    @Value("${app.admin.email}")
    private String adminEmail;

    private int eanSequencial = 1;

    public MockDataInitializer(UsuarioRepository usuarioRepository, VendedorService vendedorService,
                               ClienteService clienteService, ProdutoService produtoService,
                               CarrinhoService carrinhoService, PedidoService pedidoService,
                               AvaliacaoService avaliacaoService, PlatformTransactionManager transactionManager) {
        this.usuarioRepository = usuarioRepository;
        this.vendedorService = vendedorService;
        this.clienteService = clienteService;
        this.produtoService = produtoService;
        this.carrinhoService = carrinhoService;
        this.pedidoService = pedidoService;
        this.avaliacaoService = avaliacaoService;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Override
    public void run(String... args) {
        if (usuarioRepository.existsByEmail(VENDEDOR_TECH)) {
            return;
        }
        try {
            // Tudo em uma transação: se algo falhar, nada fica pela metade e tenta de novo no próximo start
            transactionTemplate.executeWithoutResult(status -> popular());
            log.info("Dados de demonstração criados. Logins (senha '{}'): vendedores {} e {}; clientes {} e {}",
                    SENHA, VENDEDOR_TECH, VENDEDOR_MODA, CLIENTE_ANA, CLIENTE_BRUNO);
        } catch (Exception e) {
            // Não derruba a aplicação por causa dos dados de exemplo
            log.error("Falha ao criar os dados de demonstração", e);
        }
    }

    private void popular() {
        // ---------- Lojas ----------
        vendedorService.criarVendedor(new VendedorRequestDTO("KamiStore Tech", VENDEDOR_TECH, SENHA,
                cnpj("45123987"), List.of(endereco("Av. Paulista", 1000, "Sala 12", "São Paulo", "SP", "01310-100")),
                List.of(new TelefoneRequestDTO(11, "33334444"))));
        vendedorService.criarVendedor(new VendedorRequestDTO("KamiStore Moda", VENDEDOR_MODA, SENHA,
                cnpj("38765412"), List.of(endereco("Rua Oscar Freire", 500, null, "São Paulo", "SP", "01426-001")),
                List.of(new TelefoneRequestDTO(11, "35556666"))));

        // ---------- Clientes ----------
        ClienteResponseDTO ana = clienteService.criarCliente(new ClienteRequestDTO("Ana Souza", CLIENTE_ANA, SENHA,
                cpf("529982247"), List.of(endereco("Rua das Flores", 123, "Apto 45", "Campinas", "SP", "13010-050")),
                List.of(new TelefoneRequestDTO(19, "991234567"))));
        ClienteResponseDTO bruno = clienteService.criarCliente(new ClienteRequestDTO("Bruno Lima", CLIENTE_BRUNO, SENHA,
                cpf("111444777"), List.of(endereco("Av. Atlântica", 2000, null, "Rio de Janeiro", "RJ", "22021-001")),
                List.of(new TelefoneRequestDTO(21, "998765432"))));

        // ---------- Produtos da loja Tech (sem variações) ----------
        ProdutoResponseDTO notebook = produto(VENDEDOR_TECH, "Notebook Kami Pro 15\"",
                "Intel Core i7, 16GB RAM, SSD 512GB, tela Full HD 15.6\"", "4299.90", 8, null,
                new Color(0x2B3A55), new Color(0x4F6D9A));
        ProdutoResponseDTO mouse = produto(VENDEDOR_TECH, "Mouse Gamer RGB 12000 DPI",
                "Sensor óptico, 7 botões programáveis e iluminação RGB", "149.90", 40, null,
                new Color(0x1E1E1E), new Color(0x7B2FF7));
        ProdutoResponseDTO teclado = produto(VENDEDOR_TECH, "Teclado Mecânico ABNT2",
                "Switches blue, layout ABNT2 completo e anti-ghosting", "299.90", 25, null,
                new Color(0x22313F), new Color(0x2ECC71));
        produto(VENDEDOR_TECH, "Headset Bluetooth Noise Cancelling",
                "Cancelamento de ruído ativo e bateria de 30 horas", "499.00", 15, null,
                new Color(0x3D3D3D), new Color(0xE67E22));
        produto(VENDEDOR_TECH, "Monitor 27\" 144Hz IPS",
                "QHD 2560x1440, 1ms, HDR10 e bordas finas", "1899.00", 6, null,
                new Color(0x0F2027), new Color(0x2C5364));
        produto(VENDEDOR_TECH, "Placa de Vídeo RTX 5070 12GB",
                "GDDR7, 12GB, PCIe 5.0, DLSS 4", "3999.90", 4, null,
                new Color(0x111111), new Color(0x76B900));
        produto(VENDEDOR_TECH, "SSD NVMe 1TB",
                "Leitura de até 7000MB/s, interface PCIe 4.0", "459.90", 30, null,
                new Color(0x1C2833), new Color(0x5DADE2));
        produto(VENDEDOR_TECH, "Webcam Full HD 1080p",
                "Microfone embutido e foco automático", "229.90", 0, null, // esgotado: aparece como VENDIDO
                new Color(0x2E2E2E), new Color(0x95A5A6));

        // ---------- Produtos da loja Moda (com variações de cor/tamanho) ----------
        ProdutoResponseDTO camiseta = produto(VENDEDOR_MODA, "Camiseta Básica Algodão",
                "100% algodão, gola careca, modelagem regular", "59.90", null,
                grade(new String[]{"Preto", "Branco", "Azul"}, new String[]{"P", "M", "G", "GG"}, 5, null),
                new Color(0x34495E), new Color(0x5D6D7E));
        ProdutoResponseDTO tenis = produto(VENDEDOR_MODA, "Tênis Esportivo Runner",
                "Solado em EVA, cabedal respirável, ideal para corrida", "349.90", null,
                grade(new String[]{"Preto", "Branco"}, new String[]{"38", "39", "40", "41", "42"}, 3, null),
                new Color(0xC0392B), new Color(0xE74C3C));
        produto(VENDEDOR_MODA, "Moletom Canguru com Capuz",
                "Flanelado por dentro, bolso canguru e capuz ajustável", "189.90", null,
                List.of(variacao("Cinza", "M", 6, null), variacao("Cinza", "G", 4, null),
                        variacao("Preto", "M", 5, null), variacao("Preto", "G", 0, null),
                        variacao("Verde", "GG", 2, "209.90")),
                new Color(0x4D5656), new Color(0x7F8C8D));
        produto(VENDEDOR_MODA, "Boné Aba Curva",
                "Ajuste traseiro com fivela metálica", "79.90", null,
                List.of(variacao("Preto", null, 10, null), variacao("Branco", null, 7, null),
                        variacao("Vermelho", null, 4, null)),
                new Color(0x922B21), new Color(0xCB4335));
        produto(VENDEDOR_MODA, "Calça Jeans Slim",
                "Jeans com elastano, lavagem escura", "219.90", null,
                List.of(variacao(null, "38", 4, null), variacao(null, "40", 6, null),
                        variacao(null, "42", 6, null), variacao(null, "44", 3, null),
                        variacao(null, "46", 2, "239.90")),
                new Color(0x1B4F72), new Color(0x2874A6));
        produto(VENDEDOR_MODA, "Jaqueta Corta-Vento",
                "Impermeável, leve e com bolsos com zíper", "279.90", null,
                List.of(variacao("Azul Marinho", "M", 3, null), variacao("Azul Marinho", "G", 3, null),
                        variacao("Amarelo", "M", 2, null)),
                new Color(0xB7950B), new Color(0xF1C40F));

        // ---------- Pedidos ----------
        Long enderecoAna = ana.usuario().enderecos().get(0).id();
        Long enderecoBruno = bruno.usuario().enderecos().get(0).id();

        // Ana: um pedido ENTREGUE (permite avaliar), com produtos das duas lojas
        carrinhoService.adicionarItem(CLIENTE_ANA, new ItemCarrinhoRequestDTO(mouse.id(), null, 1));
        carrinhoService.adicionarItem(CLIENTE_ANA, new ItemCarrinhoRequestDTO(camiseta.id(), variacaoId(camiseta, "Azul", "M"), 2));
        carrinhoService.adicionarItem(CLIENTE_ANA, new ItemCarrinhoRequestDTO(tenis.id(), variacaoId(tenis, "Branco", "38"), 1));
        PedidoResponseDTO entregue = pedido(CLIENTE_ANA, enderecoAna);
        avancar(entregue.id(), PedidoStatus.ENTREGUE);

        avaliacaoService.criar(CLIENTE_ANA, mouse.id(), new AvaliacaoRequestDTO(5, "Muito preciso e o RGB é lindo!"));
        avaliacaoService.criar(CLIENTE_ANA, camiseta.id(), new AvaliacaoRequestDTO(4, "Tecido ótimo, só achei um pouco comprida."));
        avaliacaoService.criar(CLIENTE_ANA, tenis.id(), new AvaliacaoRequestDTO(5, "Super confortável para correr."));

        // Ana: um pedido ENVIADO (em trânsito)
        carrinhoService.adicionarItem(CLIENTE_ANA, new ItemCarrinhoRequestDTO(teclado.id(), null, 1));
        PedidoResponseDTO enviado = pedido(CLIENTE_ANA, enderecoAna);
        avancar(enviado.id(), PedidoStatus.ENVIADO);

        // Bruno: pedido ENTREGUE + avaliação, outro AGUARDANDO_PAGAMENTO e itens no carrinho
        carrinhoService.adicionarItem(CLIENTE_BRUNO, new ItemCarrinhoRequestDTO(mouse.id(), null, 1));
        PedidoResponseDTO entregueBruno = pedido(CLIENTE_BRUNO, enderecoBruno);
        avancar(entregueBruno.id(), PedidoStatus.ENTREGUE);
        avaliacaoService.criar(CLIENTE_BRUNO, mouse.id(), new AvaliacaoRequestDTO(4, "Bom custo-benefício."));

        carrinhoService.adicionarItem(CLIENTE_BRUNO, new ItemCarrinhoRequestDTO(notebook.id(), null, 1));
        pedido(CLIENTE_BRUNO, enderecoBruno);

        carrinhoService.adicionarItem(CLIENTE_BRUNO, new ItemCarrinhoRequestDTO(camiseta.id(), variacaoId(camiseta, "Preto", "M"), 1));
        carrinhoService.adicionarItem(CLIENTE_BRUNO, new ItemCarrinhoRequestDTO(tenis.id(), variacaoId(tenis, "Preto", "42"), 1));
    }

    // ===================== Auxiliares =====================

    private ProdutoResponseDTO produto(String vendedor, String nome, String descricao, String preco, Integer quantidade,
                                       List<VariacaoRequestDTO> variacoes, Color cor1, Color cor2) {
        String ean = String.format("789%010d", 1_000_000 + eanSequencial++);
        ProdutoResponseDTO criado = produtoService.criar(vendedor,
                new ProdutoRequestDTO(nome, descricao, ean, new BigDecimal(preco), quantidade, variacoes));

        // Uma foto por cor (quando houver variações de cor), senão 3 fotos "ângulos" do produto
        List<String> cores = criado.variacoes().stream().map(VariacaoResponseDTO::cor)
                .filter(c -> c != null).distinct().toList();
        List<MultipartFile> fotos = new ArrayList<>();
        if (cores.isEmpty()) {
            String[] legendas = {"Frente", "Detalhe", "Embalagem"};
            for (int i = 0; i < legendas.length; i++) {
                fotos.add(foto(nome, legendas[i], i, cor1, cor2));
            }
        } else {
            for (int i = 0; i < cores.size(); i++) {
                Color c = corPorNome(cores.get(i), cor1);
                fotos.add(foto(nome, cores.get(i), i, c, c.brighter()));
            }
        }
        return produtoService.adicionarImagens(vendedor, criado.id(), fotos);
    }

    // Checkout com o contexto de persistência "limpo", como acontece numa requisição real.
    // Sem isso, o Hibernate não apaga os itens de um carrinho criado e esvaziado na mesma transação
    private PedidoResponseDTO pedido(String cliente, Long enderecoId) {
        entityManager.flush();
        entityManager.clear();
        PedidoResponseDTO pedido = pedidoService.criarPedido(cliente, new PedidoRequestDTO(enderecoId));
        entityManager.flush();
        entityManager.clear();
        return pedido;
    }

    private void avancar(Long pedidoId, PedidoStatus ate) {
        PedidoStatus[] fluxo = {PedidoStatus.PAGAMENTO_APROVADO, PedidoStatus.EM_PREPARACAO,
                PedidoStatus.ENVIADO, PedidoStatus.ENTREGUE};
        for (PedidoStatus status : fluxo) {
            pedidoService.atualizarStatus(adminEmail, pedidoId, status); // o ADMIN pode alterar qualquer pedido
            if (status == ate) return;
        }
    }

    private static Long variacaoId(ProdutoResponseDTO p, String cor, String tamanho) {
        return p.variacoes().stream()
                .filter(v -> cor.equals(v.cor()) && tamanho.equals(v.tamanho()))
                .findFirst().orElseThrow().id();
    }

    private static List<VariacaoRequestDTO> grade(String[] cores, String[] tamanhos, int estoque, String preco) {
        List<VariacaoRequestDTO> lista = new ArrayList<>();
        int i = 0;
        for (String cor : cores) {
            for (String tamanho : tamanhos) {
                // varia um pouco o estoque e deixa uma combinação esgotada para testar o front
                int qtd = (i++ == 2) ? 0 : estoque + (i % 3);
                lista.add(variacao(cor, tamanho, qtd, preco));
            }
        }
        return lista;
    }

    private static VariacaoRequestDTO variacao(String cor, String tamanho, int quantidade, String preco) {
        return new VariacaoRequestDTO(null, cor, tamanho, preco == null ? null : new BigDecimal(preco), quantidade);
    }

    private static EnderecoRequestDTO endereco(String rua, int numero, String complemento,
                                               String cidade, String estado, String cep) {
        return new EnderecoRequestDTO(rua, numero, complemento, cidade, estado, cep);
    }

    // Gera dígitos verificadores válidos para CPF/CNPJ a partir de uma base fixa
    private static String cpf(String base9) {
        String s = base9;
        for (int k = 0; k < 2; k++) {
            int soma = 0;
            for (int i = 0; i < s.length(); i++) soma += (s.charAt(i) - '0') * (s.length() + 1 - i);
            int dv = 11 - soma % 11;
            s += (dv > 9 ? 0 : dv);
        }
        return s;
    }

    private static String cnpj(String raiz8) {
        String s = raiz8 + "0001";
        int[][] pesos = {{5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2}, {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2}};
        for (int[] p : pesos) {
            int soma = 0;
            for (int i = 0; i < p.length; i++) soma += (s.charAt(i) - '0') * p[i];
            int r = soma % 11;
            s += (r < 2 ? 0 : 11 - r);
        }
        return s;
    }

    private static Color corPorNome(String nome, Color padrao) {
        return switch (nome.toLowerCase()) {
            case "preto" -> new Color(0x2C2C2C);
            case "branco" -> new Color(0xD9D9D9);
            case "azul" -> new Color(0x2E86C1);
            case "azul marinho" -> new Color(0x1B2A49);
            case "cinza" -> new Color(0x808B96);
            case "verde" -> new Color(0x27AE60);
            case "vermelho" -> new Color(0xC0392B);
            case "amarelo" -> new Color(0xF4D03F);
            default -> padrao;
        };
    }

    // Gera uma imagem PNG 800x800 com degradê, o nome do produto e uma legenda
    private static MultipartFile foto(String nome, String legenda, int indice, Color cor1, Color cor2) {
        int t = 800;
        BufferedImage img = new BufferedImage(t, t, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        g.setPaint(new GradientPaint(0, 0, cor1, t, t, cor2));
        g.fillRect(0, 0, t, t);

        // "Produto" estilizado: cartão claro no centro, posição muda um pouco em cada foto
        int deslocamento = indice * 30;
        g.setColor(new Color(255, 255, 255, 40));
        g.fillOval(120 + deslocamento, 120, 560 - deslocamento, 560 - deslocamento);
        g.setColor(new Color(255, 255, 255, 220));
        g.fill(new RoundRectangle2D.Double(200, 260 + deslocamento / 2.0, 400, 240, 40, 40));

        boolean fundoClaro = (cor1.getRed() * 299 + cor1.getGreen() * 587 + cor1.getBlue() * 114) / 1000 > 170;
        g.setColor(new Color(0x222222));
        desenharCentralizado(g, nome, new Font(Font.SANS_SERIF, Font.BOLD, 30), 340 + deslocamento / 2, 360);
        g.setColor(new Color(0x666666));
        desenharCentralizado(g, legenda, new Font(Font.SANS_SERIF, Font.PLAIN, 26), 440 + deslocamento / 2, 360);

        g.setColor(fundoClaro ? new Color(0x333333) : Color.WHITE);
        desenharCentralizado(g, "KamiStore", new Font(Font.SANS_SERIF, Font.BOLD, 28), 740, 760);
        g.dispose();

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ImageIO.write(img, "png", out);
            String arquivo = nome.replaceAll("[^A-Za-z0-9]+", "-").toLowerCase() + "-" + (indice + 1) + ".png";
            return new ArquivoEmMemoria(arquivo, "image/png", out.toByteArray());
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    // Quebra o texto em linhas para caber na largura e desenha centralizado
    private static void desenharCentralizado(Graphics2D g, String texto, Font fonte, int y, int largura) {
        g.setFont(fonte);
        FontMetrics fm = g.getFontMetrics();
        List<String> linhas = new ArrayList<>();
        StringBuilder atual = new StringBuilder();
        for (String palavra : texto.split(" ")) {
            String teste = atual.isEmpty() ? palavra : atual + " " + palavra;
            if (fm.stringWidth(teste) > largura && !atual.isEmpty()) {
                linhas.add(atual.toString());
                atual = new StringBuilder(palavra);
            } else {
                atual = new StringBuilder(teste);
            }
        }
        linhas.add(atual.toString());
        int inicio = y - (linhas.size() - 1) * fm.getHeight() / 2;
        for (int i = 0; i < linhas.size(); i++) {
            String linha = linhas.get(i);
            g.drawString(linha, (800 - fm.stringWidth(linha)) / 2, inicio + i * fm.getHeight());
        }
    }

    // MultipartFile simples em memória, para reaproveitar a mesma validação do upload da API
    private record ArquivoEmMemoria(String nome, String tipo, byte[] dados) implements MultipartFile {
        @Override public String getName() { return "imagens"; }
        @Override public String getOriginalFilename() { return nome; }
        @Override public String getContentType() { return tipo; }
        @Override public boolean isEmpty() { return dados.length == 0; }
        @Override public long getSize() { return dados.length; }
        @Override public byte[] getBytes() { return dados; }
        @Override public InputStream getInputStream() { return new ByteArrayInputStream(dados); }
        @Override public void transferTo(File destino) throws IOException {
            java.nio.file.Files.write(destino.toPath(), dados);
        }
    }
}
