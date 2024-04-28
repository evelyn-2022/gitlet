package gitlet;

import java.io.File;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.*;

import static gitlet.Utils.*;

/** Represents a gitlet commit object.
 *  @author Evelyn
 */
public class Commit implements Serializable {
    /** The commits directory. */
    static final File OBJECTS_FOLDER = join(Repository.GITLET_DIR, "objects");
    static final File COMMITS_FOLDER = join(OBJECTS_FOLDER, "commits");

    /** The time this Commit is created. */
    private String timestamp;
    /** The ID of the parent of this Commit .*/
    private LinkedList<String> parents;
    /** The message of this Commit. */
    private String message;
    /** The files being tracked in this Commit, with filename as keys and SHA-1 as values. */
    private HashMap<String, String> files;

    public Commit() {
        this("initial commit", new LinkedList<>(), new HashMap<>());
        this.timestamp = new SimpleDateFormat("E MMM dd hh:mm:ss yyyy Z").format(new Date(0));
    }

    public Commit(String message, LinkedList<String> parents, HashMap<String, String> files) {
        this.timestamp = new SimpleDateFormat("E MMM dd hh:mm:ss yyyy Z").format(new Date());
        this.message = message;
        this.parents = parents;
        this.files = files;
    }

    public HashMap<String, String> getFiles() {
        return this.files;
    }

    public LinkedList<String> getParents() {
        return this.parents;
    }

    public String getMessage() {
        return this.message;
    }

    public String getDate() {
        return this.timestamp;
    }

    public String persistCommit() {
        String id = sha1(serialize(this));
        File f = new File(COMMITS_FOLDER, id);
        writeObject(f, this);
        return id;
    }

    public static String getCurrentCommitId() {
        String head = readContentsAsString(join(Repository.GITLET_DIR, "HEAD"));
        if (head.length() < UID_LENGTH) {
            String activeBranch = Repository.getActiveBranch();
            head = readContentsAsString(join(Repository.HEADS_FOLDER, activeBranch));
        }
        return head;
    }

    public static Commit readCommit(String commitId) {
        File f = new File(COMMITS_FOLDER, commitId);
        return readObject(f, Commit.class);
    }

    public String toString() {
        String str = "message: " + this.message
                + "\ntimestamp: " + this.timestamp
                + "\nparent: " + this.parents;
        for (String key : files.keySet()) {
            str += "\nfile: " + key + ": " + files.get(key);
        }
        return str;
    }
}

