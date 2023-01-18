package tech.tresearchgroup.microservices.hashing.view;

import io.activej.http.HttpMethod;
import io.activej.http.HttpRequest;
import io.activej.http.HttpResponse;
import io.activej.http.RoutingServlet;
import io.activej.inject.annotation.Provides;
import io.activej.inject.module.AbstractModule;
import io.activej.promise.Promisable;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class StringEndpoints extends AbstractModule {
    @Provides
    public RoutingServlet servlet() {
        return RoutingServlet.create()
            .map(HttpMethod.GET, "/v1/:algorithm", this::hash);
    }

    private Promisable<HttpResponse> hash(HttpRequest httpRequest) {
        try {
            String string = httpRequest.getQueryParameter("string");
            String algorithm = httpRequest.getPathParameter("algorithm");
            if(string == null) {
                return HttpResponse.ofCode(500);
            }
            MessageDigest md = MessageDigest.getInstance(algorithm);
            byte[] array = md.digest(string.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : array) {
                sb.append(Integer.toHexString((b & 0xFF) | 0x100), 1, 3);
            }
            return HttpResponse.ok200().withBody(sb.toString().getBytes());
        } catch (NoSuchAlgorithmException e) {
            return HttpResponse.ofCode(500);
        }
    }
}
