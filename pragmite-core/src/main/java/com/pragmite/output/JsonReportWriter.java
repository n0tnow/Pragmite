package com.pragmite.output;

import com.google.gson.*;
import com.pragmite.model.AnalysisResult;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * JSON formatında rapor yazıcısı.
 */
public class JsonReportWriter {

    private final Gson gson;

    public JsonReportWriter() {
        this.gson = new GsonBuilder()
            .setPrettyPrinting()
            .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
            .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
            .create();
    }

    /**
     * Analiz sonucunu JSON dosyasına yazar.
     */
    public void write(AnalysisResult result, Path outputPath) throws IOException {
        String json = gson.toJson(result);
        Files.writeString(outputPath, json);
    }

    /**
     * Analiz sonucunu JSON string olarak döner.
     */
    public String toJson(AnalysisResult result) {
        return gson.toJson(result);
    }

    /**
     * LocalDateTime için TypeAdapter.
     */
    private static class LocalDateTimeAdapter implements JsonSerializer<LocalDateTime>, JsonDeserializer<LocalDateTime> {
        private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

        @Override
        public JsonElement serialize(LocalDateTime src, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(src.format(FORMATTER));
        }

        @Override
        public LocalDateTime deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                throws JsonParseException {
            return LocalDateTime.parse(json.getAsString(), FORMATTER);
        }
    }
}
