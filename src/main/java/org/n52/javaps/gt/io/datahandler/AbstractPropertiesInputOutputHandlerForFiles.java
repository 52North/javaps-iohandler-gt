package org.n52.javaps.gt.io.datahandler;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.n52.janmayen.lifecycle.Destroyable;
import org.n52.javaps.io.AbstractPropertiesInputOutputHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractPropertiesInputOutputHandlerForFiles extends AbstractPropertiesInputOutputHandler
        implements Destroyable {

    private static Logger LOGGER = LoggerFactory.getLogger(AbstractPropertiesInputOutputHandlerForFiles.class);

    /**
     * A list of files that shall be deleted by destructor. Convenience
     * mechanism to delete temporary files that had to be written during the
     * generation procedure.
     */
    protected List<File> finalizeFiles;

    public AbstractPropertiesInputOutputHandlerForFiles() {
        super();
        finalizeFiles = new ArrayList<File>();
    }

    @Override
    public void destroy() {
        if (finalizeFiles != null) {
            for (File currentFile : finalizeFiles) {
                if (!currentFile.delete()) {
                    LOGGER.trace("Could not delete file: " + currentFile);
                }
            }
        }
    }

}
