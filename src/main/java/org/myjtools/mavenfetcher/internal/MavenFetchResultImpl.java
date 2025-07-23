/**
 * @author Luis Iñesta Gelabert -  luiinge@gmail.com
 */
package org.myjtools.mavenfetcher.internal;

import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.collection.CollectResult;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.repository.LocalRepositoryManager;
import org.myjtools.mavenfetcher.FetchedArtifact;
import org.myjtools.mavenfetcher.MavenFetchResult;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class MavenFetchResultImpl implements MavenFetchResult {

    private final List<CollectResult> results;
    private final List<FetchedArtifact> rootArtifacts;


    MavenFetchResultImpl(List<CollectResult> results, DefaultRepositorySystemSession session) {
        this.results = results;
        LocalRepositoryManager localRepositoryManager = session.getLocalRepositoryManager();
        Path repositoryPath = localRepositoryManager.getRepository().getBasedir().toPath();
        this.rootArtifacts = results.stream()
                .map(CollectResult::getRoot)
                .map(root -> collectArtifact(root, localRepositoryManager, repositoryPath))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }


    private Optional<FetchedArtifact> collectArtifact(
        DependencyNode node,
        LocalRepositoryManager localRepositoryManager,
        Path repositoryPath
    ) {
        Artifact artifact = node.getArtifact();
        Path localPath = repositoryPath.resolve(
            localRepositoryManager.getPathForLocalArtifact(artifact)
        );
        // the existence of the dependency node does not imply the artifact exists
        if (!Files.exists(localPath)) {
            return Optional.empty();
        }
        List<FetchedArtifact> dependencies = node.getChildren().stream()
            .map(child -> collectArtifact(child, localRepositoryManager, repositoryPath))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toList());

        return Optional.of(new FetchedArtifact(
            artifact.getGroupId(),
            artifact.getArtifactId(),
            artifact.getVersion(),
            localPath,
            dependencies
        ));
    }


    @Override
    public Stream<FetchedArtifact> artifacts() {
        return rootArtifacts.stream();
    }


    @Override
    public Stream<FetchedArtifact> allArtifacts() {
        return Stream.concat(
            rootArtifacts.stream(),
            rootArtifacts.stream().flatMap(FetchedArtifact::allDepedencies)
        );
    }


    @Override
    public boolean hasErrors() {
        return results.stream().anyMatch(it -> !it.getExceptions().isEmpty());
    }


    @Override
    public Stream<Exception> errors() {
        return results.stream().flatMap(it -> it.getExceptions().stream());
    }


    @Override
    public String toString() {
        return artifacts().map(FetchedArtifact::toString).collect(Collectors.joining());
    }


}
