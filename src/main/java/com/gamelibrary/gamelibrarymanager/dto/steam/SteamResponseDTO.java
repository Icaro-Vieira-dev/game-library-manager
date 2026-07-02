package com.gamelibrary.gamelibrarymanager.dto.steam;

import java.util.List;

public class SteamResponseDTO {
    private SteamResponse response;


    //Getter
    public SteamResponse getResponse() {
        return response;
    }

    //Setter
    public void setResponse(SteamResponse response) {
        this.response = response;
    }

    public static class SteamResponse{
        private List<SteamGameDTO> games;


        //Getter

        public List<SteamGameDTO> getGames() {
            return games;
        }


        //Setter


        public void setGames(List<SteamGameDTO> games) {
            this.games = games;
        }
    }
}
