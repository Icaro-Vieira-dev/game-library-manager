package com.gamelibrary.gamelibrarymanager.dto.steam;

public class SteamGameDTO {
    private int appid;
    private String name;
    private int playtime_forever;
    // minutos jogados nas últimas 2 semanas; a Steam só envia se for maior que 0 (senão fica 0)
    private int playtime_2weeks;
    // última vez que jogou, em segundos desde 01/01/1970 (timestamp Unix); 0 se nunca jogou
    private long rtime_last_played;

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
    public int getPlaytime_2weeks() {
        return playtime_2weeks;
    }
    public long getRtime_last_played() {
        return rtime_last_played;
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
    public void setPlaytime_2weeks(int playtime_2weeks) {
        this.playtime_2weeks = playtime_2weeks;
    }
    public void setRtime_last_played(long rtime_last_played) {
        this.rtime_last_played = rtime_last_played;
    }
}
