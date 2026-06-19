package com.xz.nquestql.storage;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import java.io.*;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.Executors;

public class QPStorage {
    private final Path filePath;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private Map<UUID, QPData> data = new HashMap<>();
    private static final String UPLOAD_URL = "https://liyuzhen.cn/qinglan/games/qlquest/qp-upload.php";

    public QPStorage(Path path) { this.filePath = path; load(); }

    private void load() {
        File f = filePath.toFile();
        if (!f.exists()) return;
        try (FileReader fr = new FileReader(f)) {
            Type t = new TypeToken<Map<UUID, QPData>>(){}.getType();
            Map<UUID, QPData> d = gson.fromJson(fr, t);
            if (d != null) data = d;
        } catch (Exception ignored) {}
    }

    private void save() {
        try { filePath.toFile().getParentFile().mkdirs(); try (FileWriter fw = new FileWriter(filePath.toFile())) { gson.toJson(data, fw); } } catch (Exception ignored) {}
    }

    public void addQP(UUID uuid, String name, int amount) {
        QPData d = data.computeIfAbsent(uuid, k -> new QPData());
        d.qp += amount;
        d.lastName = name;
        save();
        uploadRemote(uuid, d.qp, name);
    }

    private void uploadRemote(UUID uuid, int qp, String name) {
        Executors.newSingleThreadExecutor().submit(() -> {
            try {
                URL url = new URL(UPLOAD_URL);
                HttpURLConnection c = (HttpURLConnection) url.openConnection();
                c.setRequestMethod("PUT");
                c.setDoOutput(true);
                c.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                c.setConnectTimeout(3000);
                c.setReadTimeout(3000);
                JsonObject body = new JsonObject();
                body.addProperty("uuid", uuid.toString());
                body.addProperty("qp", qp);
                body.addProperty("name", name);
                try (OutputStreamWriter ow = new OutputStreamWriter(c.getOutputStream(), "UTF-8")) { ow.write(body.toString()); }
                c.getResponseCode();
                c.disconnect();
            } catch (Exception ignored) {}
        });
    }

    public int getQP(UUID uuid) { QPData d = data.get(uuid); return d == null ? 0 : d.qp; }
    public Map<UUID, QPData> getAll() { return new HashMap<>(data); }

    public static class QPData { public int qp; public String lastName; }
}
