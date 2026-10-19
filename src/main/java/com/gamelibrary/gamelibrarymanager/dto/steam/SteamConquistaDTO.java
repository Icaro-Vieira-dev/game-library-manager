package com.gamelibrary.gamelibrarymanager.dto.steam;

// Uma conquista como a Steam devolve no GetPlayerAchievements (os nomes seguem o JSON da Steam)
public class SteamConquistaDTO {
    private String apiname;
    // 1 = desbloqueada, 0 = não
    private int achieved;
    // quando desbloqueou, em segundos desde 1970; 0 se não desbloqueou
    private long unlocktime;
    private String name;
    private String description;

    // Getter
    public String getApiname() {
        return apiname;
    }
    public int getAchieved() {
        return achieved;
    }
    public long getUnlocktime() {
        return unlocktime;
    }
    public String getName() {
        return name;
    }
    public String getDescription() {
        return description;
    }

    // Setter
    public void setApiname(String apiname) {
        this.apiname = apiname;
    }
    public void setAchieved(int achieved) {
        this.achieved = achieved;
    }
    public void setUnlocktime(long unlocktime) {
        this.unlocktime = unlocktime;
    }
    public void setName(String name) {
        this.name = name;
    }
    public void setDescription(String description) {
        this.description = description;
    }
}
