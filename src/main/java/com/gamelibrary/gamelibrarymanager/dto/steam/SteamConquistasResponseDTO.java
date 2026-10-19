package com.gamelibrary.gamelibrarymanager.dto.steam;

import java.util.List;

// Formato da resposta do GetPlayerAchievements:
// { "playerstats": { "success": true, "achievements": [ ... ] } }
// ou, em caso de erro: { "playerstats": { "success": false, "error": "Requested app has no stats" } }
public class SteamConquistasResponseDTO {
    private PlayerStats playerstats;

    //Getter
    public PlayerStats getPlayerstats() {
        return playerstats;
    }

    //Setter
    public void setPlayerstats(PlayerStats playerstats) {
        this.playerstats = playerstats;
    }

    public static class PlayerStats {
        private boolean success;
        private String error;
        private List<SteamConquistaDTO> achievements;

        //Getter
        public boolean isSuccess() {
            return success;
        }
        public String getError() {
            return error;
        }
        public List<SteamConquistaDTO> getAchievements() {
            return achievements;
        }

        //Setter
        public void setSuccess(boolean success) {
            this.success = success;
        }
        public void setError(String error) {
            this.error = error;
        }
        public void setAchievements(List<SteamConquistaDTO> achievements) {
            this.achievements = achievements;
        }
    }
}
