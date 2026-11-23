package blackseq.card;

/** What {@link ProjectFolder#writePattern} does when the pattern file exists. */
public enum WriteMode {
    /** Stop with a {@link java.nio.file.FileAlreadyExistsException}. Nothing is changed. */
    FAIL_IF_EXISTS,
    /** Copy the file to the backup folder of the project, then write the new content. */
    BACKUP_THEN_REPLACE
}
