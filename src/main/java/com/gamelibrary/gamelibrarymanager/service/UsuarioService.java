package com.gamelibrary.gamelibrarymanager.service;

import com.gamelibrary.gamelibrarymanager.SegurancaConfig;
import com.gamelibrary.gamelibrarymanager.dto.CadastroRequestDTO;
import com.gamelibrary.gamelibrarymanager.dto.LoginRequestDTO;
import com.gamelibrary.gamelibrarymanager.dto.TokenResponseDTO;
import com.gamelibrary.gamelibrarymanager.exception.CredenciaisInvalidasException;
import com.gamelibrary.gamelibrarymanager.exception.EmailJaCadastradoException;
import com.gamelibrary.gamelibrarymanager.exception.SteamIdJaVinculadoException;
import com.gamelibrary.gamelibrarymanager.model.Usuario;
import com.gamelibrary.gamelibrarymanager.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

// Cadastro e login de usuários.
// É o ÚNICO lugar da aplicação que gera hash de senha (passwordEncoder.encode) e confere senha
// (passwordEncoder.matches). Nenhum método daqui loga, devolve ou coloca em mensagem a senha,
// o hash ou o token.
@Service
public class UsuarioService {
    private final UsuarioRepository usuarioRepository;
    // Argon2id (SegurancaConfig)
    private final PasswordEncoder passwordEncoder;
    private final JwtEncoder jwtEncoder;
    private final LimiteTentativasLogin limiteTentativas;
    private final Clock relogio;
    private final Duration validadeToken;
    // Hash de uma senha aleatória que ninguém conhece, gerado uma vez quando o service é criado.
    // Usado no login quando o e-mail não existe (veja o comentário de TEMPO CONSTANTE no login).
    private final String hashFalso;

    @Autowired
    public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder,
                          JwtEncoder jwtEncoder, LimiteTentativasLogin limiteTentativas, Clock relogio,
                          @Value("${jwt.expiracao-minutos:60}") long expiracaoMinutos) {
        if (expiracaoMinutos <= 0) {
            throw new IllegalStateException("jwt.expiracao-minutos (JWT_EXPIRACAO_MINUTOS) precisa ser maior que zero.");
        }
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtEncoder = jwtEncoder;
        this.limiteTentativas = limiteTentativas;
        this.relogio = relogio;
        this.validadeToken = Duration.ofMinutes(expiracaoMinutos);
        // UUID aleatório (gerado com SecureRandom). A senha em si é descartada: só o hash fica.
        this.hashFalso = passwordEncoder.encode(UUID.randomUUID().toString());
    }

    // Cadastra um usuário. A senha vira hash Argon2id aqui e só o hash é salvo.
    // nome e steamId já chegam sem espaços nas pontas (construtor do CadastroRequestDTO);
    // steamId é null quando o usuário não informou.
    // E-mail já cadastrado -> EmailJaCadastradoException (409).
    // Steam ID já usado por outra conta -> SteamIdJaVinculadoException (409).
    public Usuario cadastrar(CadastroRequestDTO dto) {
        String email = Usuario.normalizarEmail(dto.email());
        String steamId = dto.steamId();

        // as duas conferências vêm ANTES do hash: não gasta Argon2 com um cadastro que vai ser recusado
        if (usuarioRepository.existsByEmail(email)) {
            throw new EmailJaCadastradoException();
        }
        if (steamId != null && usuarioRepository.existsBySteamId(steamId)) {
            throw new SteamIdJaVinculadoException();
        }

        // AQUI a senha vira hash (Argon2id, com um "sal" aleatório diferente para cada senha)
        String senhaHash = passwordEncoder.encode(dto.senha());
        Usuario usuario = new Usuario(dto.nome(), email, steamId, senhaHash, OffsetDateTime.now(relogio));

        try {
            return usuarioRepository.save(usuario);
        } catch (DataIntegrityViolationException e) {
            // Dois cadastros ao mesmo tempo com o mesmo e-mail ou o mesmo Steam ID: os dois passaram
            // pelas conferências acima, e o UNIQUE do banco recusou o segundo. Descobre QUAL dos dois
            // bateu (e não outro problema) antes de responder 409; se não foi nenhum, deixa o erro
            // seguir (vira 500, com log).
            if (usuarioRepository.existsByEmail(email)) {
                throw new EmailJaCadastradoException();
            }
            if (steamId != null && usuarioRepository.existsBySteamId(steamId)) {
                throw new SteamIdJaVinculadoException();
            }
            throw e;
        }
    }

    // Confere e-mail e senha e devolve um JWT.
    // ipDeQuemChamou: request.getRemoteAddr() (usado só no limite de tentativas).
    // E-mail inexistente ou senha errada -> CredenciaisInvalidasException (401), a MESMA nos dois casos.
    // Muitas tentativas -> TentativasExcedidasException (429).
    public TokenResponseDTO login(LoginRequestDTO dto, String ipDeQuemChamou) {
        String email = Usuario.normalizarEmail(dto.email());

        // limite de tentativas: conta esta e lança 429 se passou do limite. Vem ANTES do Argon2,
        // então quem está bloqueado nem gasta processamento do servidor.
        String chaveLimite = LimiteTentativasLogin.chave(email, ipDeQuemChamou);
        limiteTentativas.consumirTentativa(chaveLimite);

        Optional<Usuario> usuario = usuarioRepository.findByEmail(email);

        // TEMPO CONSTANTE: o Argon2 é lento de propósito (dezenas de milissegundos). Se, para e-mail
        // inexistente, a resposta voltasse na hora (sem rodar o Argon2), bastaria medir quanto cada
        // resposta demora para descobrir quais e-mails estão cadastrados. Por isso o matches roda SEMPRE:
        // contra o hash do usuário, ou contra o hashFalso quando o e-mail não existe.
        String hashParaComparar = usuario.map(Usuario::getSenhaHash).orElse(hashFalso);
        boolean senhaConfere = passwordEncoder.matches(dto.senha(), hashParaComparar);

        if (usuario.isEmpty() || !senhaConfere) {
            throw new CredenciaisInvalidasException();
        }

        limiteTentativas.registrarSucesso(chaveLimite);
        return gerarToken(usuario.get());
    }

    // Usado pelo GET /usuarios/me: o id vem do "sub" de um token já verificado pelo Spring.
    // Se o usuário não existe mais (token válido de uma conta apagada), responde 401 como token inválido.
    public Usuario buscarPorId(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new InvalidBearerTokenException("Token inválido."));
    }

    // AQUI o JWT é GERADO (e assinado com a chave JWT_SECRET, HS256).
    // Conteúdo: iss (quem emitiu), sub (id do usuário), iat (quando foi emitido) e exp (quando vence).
    // NADA sensível vai aqui dentro, nem o e-mail: o JWT é só codificado em Base64, qualquer pessoa
    // que tiver o token consegue LER o conteúdo. A assinatura só impede que alguém o ALTERE.
    private TokenResponseDTO gerarToken(Usuario usuario) {
        Instant agora = relogio.instant();
        Instant expiraEm = agora.plus(validadeToken);

        JwtClaimsSet conteudo = JwtClaimsSet.builder()
                .issuer(SegurancaConfig.EMISSOR_JWT)
                .subject(String.valueOf(usuario.getId()))
                .issuedAt(agora)
                .expiresAt(expiraEm)
                .build();
        JwsHeader cabecalho = JwsHeader.with(MacAlgorithm.HS256).build();

        String token = jwtEncoder.encode(JwtEncoderParameters.from(cabecalho, conteudo)).getTokenValue();
        return TokenResponseDTO.bearer(token, expiraEm.atOffset(ZoneOffset.UTC));
    }
}
