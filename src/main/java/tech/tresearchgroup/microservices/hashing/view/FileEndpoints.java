package tech.tresearchgroup.microservices.hashing.view;

import io.activej.csp.file.ChannelFileWriter;
import io.activej.http.*;
import io.activej.inject.annotation.Provides;
import io.activej.inject.module.AbstractModule;
import io.activej.promise.Promisable;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;
import java.util.concurrent.Executor;

import static java.util.concurrent.Executors.newSingleThreadExecutor;

public class FileEndpoints extends AbstractModule {
    @Provides
    public RoutingServlet servlet() {
        return RoutingServlet.create()
            .map(HttpMethod.POST, "/v1/:algorithm", this::handleUpload);
    }

    @Provides
    static Executor executor() {
        return newSingleThreadExecutor();
    }

    public @NotNull Promisable<HttpResponse> handleUpload(HttpRequest httpRequest) {
        try {
            UUID uuid = UUID.randomUUID();
            Path file = new File("temp/" + uuid + ".tmp").toPath();
            String algorithm = httpRequest.getPathParameter("algorithm");
            return httpRequest.handleMultipart(MultipartDecoder.MultipartDataHandler.file(fileName ->
                    ChannelFileWriter.open(executor(), file)))
                .map($ -> hash(file, algorithm));
        } catch (Exception e) {
            return HttpResponse.ofCode(500);
        }
    }

    private HttpResponse hash(Path path, String algorithm) throws IOException, NoSuchAlgorithmException {
        byte [] buffer = new byte[1024];
        MessageDigest md = MessageDigest.getInstance(algorithm);
        InputStream in = new FileInputStream(path.toFile());
        int sizeRead;
        while ((sizeRead = in.read(buffer)) != -1) {
            md.update(buffer, 0, sizeRead);
        }
        in.close();

        byte [] hash = md.digest();
        if(!path.toFile().delete()) {
            System.err.println("Failed to delete: " + path);
        }
        StringBuilder sb = new StringBuilder();
        for (byte b : hash) {
            sb.append(Integer.toHexString((b & 0xFF) | 0x100), 1, 3);
        }
        return HttpResponse.ok200().withBody(sb.toString().getBytes());
    }
}
