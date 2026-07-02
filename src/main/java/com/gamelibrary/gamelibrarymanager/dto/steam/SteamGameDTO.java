package com.gamelibrary.gamelibrarymanager.dto.steam;

public class SteamGameDTO {
    private int appid;
    private String name;
    private int playtime_forever;

    // Getter
    public int getAppid() {
        return appid;
    }
    public String getName() {
        return name;
    }
    public int getPlaytime_forever() {
        return playtime_forever;
    }

    //Setter
    public void setAppid(int appid) {
        this.appid = appid;
    }
    public void setName(String name) {
        this.name = name;
    }
    public void setPlaytime_forever(int playtime_forever) {
        this.playtime_forever = playtime_forever;
    }
}
