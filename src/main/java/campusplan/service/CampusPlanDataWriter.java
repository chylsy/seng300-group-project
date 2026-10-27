
package campusplan.service;

import campusplan.domain.CampusPlanData;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class CampusPlanDataWriter {

    private final Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    public void save(CampusPlanData data, Path path) throws IOException {
        Path absolutePath = path.toAbsolutePath();
        Path parent = absolutePath.getParent();

        if (parent != null) {
            Files.createDirectories(parent);
        }

        String json = gson.toJson(data);

        Files.writeString(
                absolutePath,
                json,
                StandardCharsets.UTF_8
        );
    }
}
