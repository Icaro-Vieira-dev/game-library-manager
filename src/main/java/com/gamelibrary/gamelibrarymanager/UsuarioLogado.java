package com.gamelibrary.gamelibrarymanager;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// O jeito ÚNICO de um controller saber quem está logado:
//
//   public JogoResponseDTO buscarPorId(@PathVariable Long id, @UsuarioLogado Usuario usuario) { ... }
//
// O Spring preenche o parâmetro com o usuário dono do token (UsuarioLogadoResolver). O id vem SEMPRE do
// "sub" do JWT, que o Spring Security já verificou; NUNCA da URL, do body nem de um parâmetro.
// Sem login (ou com um token de conta que não existe mais) -> 401.
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface UsuarioLogado {
}
