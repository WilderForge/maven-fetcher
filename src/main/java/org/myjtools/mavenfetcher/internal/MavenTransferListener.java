/**
 * @author Luis Iñesta Gelabert -  luiinge@gmail.com
 */
package org.myjtools.mavenfetcher.internal;


import org.eclipse.aether.transfer.TransferCancelledException;
import org.eclipse.aether.transfer.TransferEvent;
import org.eclipse.aether.transfer.TransferListener;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;


public class MavenTransferListener implements TransferListener {


    private final Logger logger;
    private final List<String> succededTransfers = new ArrayList<>();
    private final List<String> failedTransfers = new ArrayList<>();


    public MavenTransferListener(Logger logger) {
        Objects.requireNonNull(logger);
        this.logger = logger;
    }


    public List<String> succededTransfers() {
        return Collections.unmodifiableList(succededTransfers);
    }


    public List<String> failedTransfers() {
        return Collections.unmodifiableList(failedTransfers);
    }


    @Override
    public void transferInitiated(TransferEvent event) throws TransferCancelledException {
        //
    }


    @Override
    public void transferStarted(TransferEvent event) throws TransferCancelledException {
        if (event.getResource().getResourceName().endsWith(".jar") && logger.isInfoEnabled()) {
            logger.debug(
                    "Transferring {} [{}] from {}  ...",
                    resourceName(event),
                    resourceSize(event),
                    repository(event)
            );
        }
    }


    @Override
    public void transferProgressed(TransferEvent event) throws TransferCancelledException {

    }


    @Override
    public void transferCorrupted(TransferEvent event) throws TransferCancelledException {
        if (event.getResource().getResourceName().endsWith(".jar") && logger.isErrorEnabled()) {
            logger.error("Checksum validation failed for [{}]", resourceName(event));
        }
    }


    @Override
    public void transferSucceeded(TransferEvent event) {
        if (event.getResource().getResourceName().endsWith(".jar")) {
            this.succededTransfers.add(resourceNameTrimmed(event));
            this.failedTransfers.remove(resourceNameTrimmed(event));
            if (logger.isInfoEnabled() && event.getResource().getContentLength() > 0) {
                logger.info(
                        "{} [{}] downloaded from {} ",
                        resourceName(event),
                        resourceSize(event),
                        repository(event)
                );
            }
        }
    }


    @Override
    public void transferFailed(TransferEvent event) {
        if (event.getResource().getResourceName().endsWith(".jar")) {
            this.failedTransfers.add(resourceNameTrimmed(event));
            if (logger.isErrorEnabled()) {
                logger.warn(
                        "Cannot download {} from {}",
                        resourceName(event),
                        event.getResource().getRepositoryUrl()
                );
            }
        }
    }


    private String resourceName(TransferEvent event) {
        return String.format("%-80s", resourceNameTrimmed(event));
    }


    private String resourceNameTrimmed(TransferEvent event) {
        int index = event.getResource().getResourceName().lastIndexOf('/');
        return event.getResource().getResourceName().substring(index < 0 ? 0 : index + 1);
    }


    private String resourceSize(TransferEvent event) {
        long size = event.getResource().getContentLength();
        return String.format("%7s", size > 1000L ? size / 1000L + " Kb" : size + " bytes");
    }


    private String repository(TransferEvent event) {
        return event.getResource().getRepositoryUrl();
    }
}
