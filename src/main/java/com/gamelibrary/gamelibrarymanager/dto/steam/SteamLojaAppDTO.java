package com.gamelibrary.gamelibrarymanager.dto.steam;

import java.util.List;

// Resposta da loja da Steam (store.steampowered.com/api/appdetails) para UM jogo.
// O JSON vem dentro de uma chave com o próprio appid: { "1245620": { "success": true, "data": { ... } } }
// Jogo que não existe mais na loja: { "999": { "success": false } } (sem "data")
public class SteamLojaAppDTO {
    private boolean success;
    private Dados data;

    //Getter
    public boolean isSuccess() {
        return success;
    }
    public Dados getData() {
        return data;
    }

    //Setter
    public void setSuccess(boolean success) {
        this.success = success;
    }
    public void setData(Dados data) {
        this.data = data;
    }

    // só os campos que usamos; o Jackson ignora o resto do JSON
    public static class Dados {
        private List<Genero> genres;
        private List<String> developers;
        private DataLancamento release_date;
        // URL do banner horizontal; nos jogos novos o caminho tem um código no meio, por isso vale guardar a URL pronta
        private String header_image;

        //Getter
        public String getHeader_image() {
            return header_image;
        }
        public void setHeader_image(String header_image) {
            this.header_image = header_image;
        }
        public List<Genero> getGenres() {
            return genres;
        }
        public List<String> getDevelopers() {
            return developers;
        }
        public DataLancamento getRelease_date() {
            return release_date;
        }

        //Setter
        public void setGenres(List<Genero> genres) {
            this.genres = genres;
        }
        public void setDevelopers(List<String> developers) {
            this.developers = developers;
        }
        public void setRelease_date(DataLancamento release_date) {
            this.release_date = release_date;
        }
    }

    // ex.: { "id": "3", "description": "RPG" }
    public static class Genero {
        private String description;

        public String getDescription() {
            return description;
        }
        public void setDescription(String description) {
            this.description = description;
        }
    }

    // ex.: { "coming_soon": false, "date": "24/fev./2022" } (o formato da data muda com o idioma)
    public static class DataLancamento {
        private String date;

        public String getDate() {
            return date;
        }
        public void setDate(String date) {
            this.date = date;
        }
    }
}
