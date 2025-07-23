/*
  @author Luis Iñesta Gelabert -  luiinge@gmail.com
 */
package org.myjtools.mavenfetcher.test;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.myjtools.mavenfetcher.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.NoSuchElementException;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;


class TestMavenFetcher {

    private final String mockRepo = Paths.get("src", "test", "resources", "mock_maven_repo")
            .toAbsolutePath()
            .toUri()
            .toString();

    private Path localRepo;

    @BeforeEach
    public void prepareLocalRepo() throws IOException {
        localRepo = Files.createTempDirectory("test");
    }

    @AfterEach
    public void cleanLocalRepo() throws IOException {
        Files.walkFileTree(localRepo, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException {
                Files.delete(path);
                return FileVisitResult.CONTINUE;
            }
        });
    }


    @Test
    @DisplayName("Artifact can be fetched with its dependencies")
    void fetchArtifactWithDependencies() {
        MavenFetchResult result = new MavenFetcher()
                .localRepositoryPath(localRepo.toString())
                .logger(LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME))
                .fetchArtifacts(
                        new MavenFetchRequest("org.apache.maven:maven-artifact:3.9.1").scopes("compile")
                );
        System.out.println(result);
        assertThat(result.artifacts()).containsExactly(
                new FetchedArtifact("org.apache.maven:maven-artifact:3.9.1",
                        new FetchedArtifact("org.codehaus.plexus:plexus-utils:3.5.1"),
                        new FetchedArtifact("org.apache.commons:commons-lang3:3.8.1")
                )
        );

    }


    @Test
    @DisplayName("Artifact can be fetched excluding certain dependencies")
    void fetchArtifactWithExcludedDependencies() {
        MavenFetchResult result = new MavenFetcher()
                .localRepositoryPath(localRepo.toString())
                .logger(LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME))
                .fetchArtifacts(
                        new MavenFetchRequest("org.apache.maven:maven-artifact:3.9.1")
                                .scopes("compile")
                                .excludingArtifacts("org.codehaus.plexus:plexus-utils")
                );
        System.out.println(result);
        assertThat(result.artifacts()).containsExactly(
                new FetchedArtifact("org.apache.maven:maven-artifact:3.9.1",
                        new FetchedArtifact("org.apache.commons:commons-lang3:3.8.1")
                )
        );
    }


    @Test
    @DisplayName("Latest artifact version is fetched when version is not specified")
    void fetchLatestVersionIfVersionNotSpecified() {
        MavenFetchResult result = new MavenFetcher()
                .localRepositoryPath(localRepo.toString())
                .logger(LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME))
                .fetchArtifacts(
                        new MavenFetchRequest("org.apache.maven:maven-artifact").scopes("compile")
                );
        System.out.println(result);
        assertThat(result.artifacts())
                .anyMatch(it -> it.groupId().equals("org.apache.maven") && it.artifactId().equals("maven-artifact"));
    }


    @Test
    @DisplayName("Artifact version can have a profile (such as 33.1.0-jre)")
    void fetchArtifactWithRequiredProfile() {
        MavenFetchResult result = new MavenFetcher()
                .localRepositoryPath(localRepo.toString())
                .logger(LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME))
                .fetchArtifacts(
                        new MavenFetchRequest("com.google.guava:guava:33.1.0-jre").scopes("compile")
                );
        System.out.println(result);
        assertThat(result.allArtifacts()).size().isGreaterThan(1);
    }


    @Test
    @DisplayName("The default Maven remote repository can be disabled")
    void doNotUseDefaultRemoteRepository() {
        Properties withoutDefaultRepo = new Properties();
        withoutDefaultRepo.setProperty(MavenFetcherProperties.USE_DEFAULT_REMOTE_REPOSITORY, "false");
        MavenFetcher fetcher1 = new MavenFetcher().config(withoutDefaultRepo);
        assertThat(fetcher1.remoteRepositories()).isEmpty();

        Properties withDefaultRepo = new Properties();
        withoutDefaultRepo.setProperty(MavenFetcherProperties.USE_DEFAULT_REMOTE_REPOSITORY, "true");
        MavenFetcher fetcher2 = new MavenFetcher().config(withDefaultRepo);
        assertThat(fetcher2.remoteRepositories()).containsExactly(
                "maven-central (https://repo.maven.apache.org/maven2, default, releases+snapshots)"
        );

        MavenFetcher fetcher3 = new MavenFetcher();
        assertThat(fetcher3.remoteRepositories()).containsExactly(
                "maven-central (https://repo.maven.apache.org/maven2, default, releases+snapshots)"
        );
    }


    @Test
    @DisplayName("Repository URL accepts both <id=url> and <id=url [user:pwd]>")
    void repositoryFormats() {
        assertThat(new MavenFetcher().config(properties(
                MavenFetcherProperties.REMOTE_REPOSITORIES,
                "maven-central=https://repo1.maven.org/maven2"
        ))).isNotNull();
        assertThat(new MavenFetcher().config(properties(
                MavenFetcherProperties.REMOTE_REPOSITORIES,
                "maven-central=https://repo1.maven.org/maven2 [user123_@domain:mypass#123@!.]])]"
        ))).isNotNull();
    }


    @Test
    @DisplayName("When malformed repository URL is passed it throws an error")
    void malformedPropertiesThrowError() {
        assertThatCode(() -> {
            Properties properties = new Properties();
            properties.setProperty(MavenFetcherProperties.REMOTE_REPOSITORIES, "mock:file://repository");
            new MavenFetcher().config(properties);
        }).hasMessage("Invalid value for property 'remoteRepositories' : Invalid repository value 'mock:file://repository'\n" +
                "Expected formats are 'id=url' and 'id=url [user:pwd]'");
    }


    @Test
    @DisplayName("Attempting to fetch a non-existing artifact return a result with errors")
    void attemptToFetchANonExistingArtifact() {
        MavenFetchResult result = new MavenFetcher()
                .localRepositoryPath(localRepo.toString())
                .clearRemoteRepositories()
                .addRemoteRepository(new Repository("mock", mockRepo).priority(0))
                .logger(LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME))
                .fetchArtifacts(
                        new MavenFetchRequest("a:b:1.0").scopes("compile")
                );
        assertThat(result.allArtifacts()).isEmpty();
        assertThat(result.hasErrors()).isTrue();
        assertThat(result.errors().findAny().map(Exception::getMessage).orElseThrow(NoSuchElementException::new))
                .isEqualTo("Could not fetch artifact b-1.0.jar");

    }


    private Properties properties(String... pairs) {
        Properties properties = new Properties();
        for (int i = 0; i < pairs.length - 1; i += 2) {
            properties.setProperty(pairs[i], pairs[i + 1]);
        }
        return properties;
    }

}
