/**
 * @author Luis Iñesta Gelabert -  luiinge@gmail.com
 */
package org.myjtools.mavenfetcher.internal;

import org.myjtools.mavenfetcher.MavenFetcher;
import org.myjtools.mavenfetcher.Repository;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Properties;


public class MavenFetcherConfig {

    public static final String REMOTE_REPOSITORIES = "remoteRepositories";
    public static final String LOCAL_REPOSITORY = "localRepository";
    public static final String PROXY_URL = "proxy.url";
    public static final String PROXY_USERNAME = "proxy.username";
    public static final String PROXY_PASSWORD = "proxy.password";
    public static final String PROXY_EXCEPTIONS = "proxy.exceptions";

    private final Properties properties;


    public MavenFetcherConfig(String configFile) throws IOException {
        try (Reader reader = new InputStreamReader(
                new FileInputStream(configFile), StandardCharsets.UTF_8
        )) {
            this.properties = new Properties();
            this.properties.load(reader);
        }
    }


    public MavenFetcherConfig(Properties properties) {
        this.properties = properties;
    }


    public void config(MavenFetcher fetcher) throws URISyntaxException {
        String remoteRepositories = properties.getProperty(REMOTE_REPOSITORIES);
        if (remoteRepositories != null) {
            for (String remoteRepository : remoteRepositories.split(";")) {
                fetcher.addRemoteRepository(new Repository(remoteRepository, remoteRepository));
            }
        }
        String localRepository = properties.getProperty(LOCAL_REPOSITORY);
        if (localRepository != null) {
            fetcher.localRepositoryPath(localRepository);
        }
        String proxyUrl = properties.getProperty(PROXY_URL);
        if (proxyUrl != null) {
            fetcher.proxyURL(proxyUrl);
            String proxyUsername = properties.getProperty(PROXY_USERNAME);
            String proxyPassword = properties.getProperty(PROXY_PASSWORD);
            if (proxyUsername != null && proxyPassword != null) {
                fetcher.proxyCredentials(proxyUsername, proxyPassword);
            }
            String proxyExceptions = properties.getProperty(PROXY_EXCEPTIONS);
            if (proxyExceptions != null) {
                fetcher.proxyExceptions(Arrays.asList(proxyExceptions.split(";")));
            }
        }
    }

}
