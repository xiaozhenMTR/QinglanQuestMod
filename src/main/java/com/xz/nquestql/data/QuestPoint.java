package com.xz.nquestql.data;

import com.google.gson.JsonObject;

public class QuestPoint {
    public int code;
    public String bossbarText;

    public static QuestPoint fromJson(JsonObject obj) {
        QuestPoint p = new QuestPoint();
        p.code = obj.has("code") ? obj.get("code").getAsInt() : 0;
        p.bossbarText = obj.has("bossbarText") ? obj.get("bossbarText").getAsString() : "";
        return p;
    }
}
