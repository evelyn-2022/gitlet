package gitlet;

import java.io.File;
import java.nio.file.Files;
import java.util.*;

import static gitlet.Utils.*;

/**
 * Represents a gitlet repository.
 * Performs all operations from command line arguments.
 * @author Evelyn
 */
public class Repository {

    /**
     * The current working directory.
     */
    public static final File CWD = new File(System.getProperty("user.dir"));
    /**
     * The .gitlet directory.
     */
    public static final File GITLET_DIR = join(CWD, ".gitlet");
    /**
     * The object directory for storing commits and blobs
     */
    static final File OBJECTS_FOLDER = join(GITLET_DIR, "objects");
    /**
     * The refs directory for storing local and remote branches
     */
    static final File HEADS_FOLDER = join(GITLET_DIR, "refs", "heads");

    //======================================================================
    // Helper functions
    //======================================================================

    private static void setupPersistence(File path) {
        File d = new File(path.toURI());
        d.mkdirs();
    }

    private static void createFile(String filename, String fileContent) {
        File filePath = new File(CWD, filename);
        writeContents(filePath, fileContent);
    }

    private static void persistBlob(File f, String fileId) {
        File filePath = join(Commit.OBJECTS_FOLDER, fileId);
        String fileContent = readContentsAsString(f);
        writeContents(filePath, fileContent);
    }

    public static String getActiveBranch() {
        String head = readContentsAsString(join(Repository.GITLET_DIR, "HEAD"));
        return head.substring("refs/heads".length() + 1);
    }

    private static void setActiveBranchPointer(String id) {
        writeContents(join(HEADS_FOLDER, getActiveBranch()), id);
    }

    //======================================================================
    // End of helper functions
    //======================================================================

    public static void init() {
        // 0. Check if gitlet already exists.
        if (Files.exists(GITLET_DIR.toPath())) {
            System.out.println("A Gitlet version-control system "
                    + "already exists in the current directory.");
            return;
        }

        // 1. Create .gitlet directory in CWD.
        setupPersistence(GITLET_DIR);

        // 2. Create objects directory in .gitlet.
        setupPersistence(Commit.COMMITS_FOLDER);

        // 3. Make initial commit.
        Commit c = new Commit();
        String id = c.persistCommit();

        // 4. Create and persist master branch.
        setupPersistence(HEADS_FOLDER);
        writeContents(join(HEADS_FOLDER, "master"), id);

        // 5. Create and persist HEAD.
        writeContents(join(GITLET_DIR, "HEAD"), join("refs", "heads", "master").toString());

        // 6. Create and persist stage.
        new Stage().persistStage();
    }

    public static void add(String filename) {
        if (!GITLET_DIR.exists()) {
            System.out.println("Not in an initialized Gitlet directory.");
            return;
        }

        Stage stage = Stage.getStage();
        List<String> files = plainFilenamesIn(CWD);

        // 0. Check if file exists.
        if (files == null || !files.contains(filename)) {
            System.out.println("File does not exist.");
            return;
        }

        // 1. Get the SHA-1 string of current file.
        File f = new File(CWD, filename);
        String currentFileId = sha1(readContents(f));

        // 2. Check if identical to the version in current commit.
        String head = Commit.getCurrentCommitId();
        HashMap<String, String> committedFiles = Commit.readCommit(head).getFiles();

        // 3. If not identical, stage for addition. If exists in staging area, overwrite it;
        // If identical, unstage from toBeAdded if it's already there.
        if (committedFiles.get(filename) == null
                || !committedFiles.get(filename).equals(currentFileId)) {
            stage.getToBeAdded().put(filename, currentFileId);
            persistBlob(f, currentFileId);
        } else {
            stage.getToBeAdded().remove(filename);
        }

        // 5. If staged for removal, unstage from toBeRemoved.
        if (stage.getToBeRemoved().contains(filename)) {
            stage.getToBeRemoved().remove(filename);
        }

        // 6. Save stage to .gitlet directory.
        stage.persistStage();
    }

    public static void commit(String message, String secParentId) {
        if (!GITLET_DIR.exists()) {
            System.out.println("Not in an initialized Gitlet directory.");
            return;
        }

        if (message.length() == 0) {
            System.out.println("Please enter a commit message.");
            return;
        }

        Stage stage = Stage.getStage();
        HashMap<String, String> tobeAdded = stage.getToBeAdded();
        LinkedList<String> tobeRemoved = stage.getToBeRemoved();

        // 0. If no files have been staged, abort.
        if (tobeAdded.size() == 0 && tobeRemoved.size() == 0) {
            System.out.println("No changes added to the commit.");
            return;
        }

        // 1. Set commit files.
        String currentCommitId = Commit.getCurrentCommitId();
        HashMap<String, String> currentCommittedFiles
                = Commit.readCommit(currentCommitId).getFiles();
        currentCommittedFiles.putAll(tobeAdded);
        for (String key : tobeRemoved) {
            currentCommittedFiles.remove(key);
        }

        // 2. Persist new commit.
        LinkedList<String> parents = new LinkedList<>();
        parents.add(currentCommitId);
        if (secParentId != null) {
            parents.add(secParentId);
        }
        Commit newCommit = new Commit(message, parents, currentCommittedFiles);
        String id = newCommit.persistCommit();

        // 3. Set branch pointer.
        setActiveBranchPointer(id);

        // 4. Clear staging area.
        new Stage().persistStage();
    }

    public static void remove(String filename) {
        if (!GITLET_DIR.exists()) {
            System.out.println("Not in an initialized Gitlet directory.");
            return;
        }

        Stage stage = Stage.getStage();
        HashMap<String, String> tobeAdded = stage.getToBeAdded();
        LinkedList<String> tobeRemoved = stage.getToBeRemoved();
        String currentCommitId = Commit.getCurrentCommitId();
        HashMap<String, String> currentCommittedFiles
                = Commit.readCommit(currentCommitId).getFiles();

        // 0. Neither staged nor tracked, no reason to remove.
        if (!tobeAdded.containsKey(filename) && !currentCommittedFiles.containsKey(filename)) {
            System.out.println("No reason to remove the file.");
            return;
        }

        // 1. Unstage the file if it is currently staged for addition.
        tobeAdded.remove(filename);
        stage.persistStage();

        // 2. Stage for removal if tracked in current commit, and remove from CWD.
        if (currentCommittedFiles.containsKey(filename) && !tobeRemoved.contains(filename)) {
            tobeRemoved.add(filename);
            stage.persistStage();
            restrictedDelete(filename);
        }
    }

    public static void log() {
        if (!GITLET_DIR.exists()) {
            System.out.println("Not in an initialized Gitlet directory.");
            return;
        }

        printCommit(Commit.getCurrentCommitId());
    }

    private static String printCommit(String id) {
        Commit currentCommit = Commit.readCommit(id);
        commitFormatter(id);
        if (currentCommit.getParents().size() == 0) {
            return null;
        }
        return printCommit(currentCommit.getParents().get(0));
    }

    private static void commitFormatter(String id) {
        Commit currentCommit = Commit.readCommit(id);
        if (currentCommit.getParents().size() < 2) {

            System.out.printf("===\n"
                            + "commit %s\n"
                            + "Date: %s\n"
                            + "%s\n\n",
                    id, currentCommit.getDate(), currentCommit.getMessage());
        } else {
            System.out.printf("===\n"
                            + "commit %s\n"
                            + "Merge: %s %s\n"
                            + "Date: %s\n"
                            + "%s\n\n",
                    id,
                    currentCommit.getParents().get(0).substring(0, 7),
                    currentCommit.getParents().get(1).substring(0, 7),
                    currentCommit.getDate(),
                    currentCommit.getMessage());
        }

    }

    public static void globalLog() {
        if (!GITLET_DIR.exists()) {
            System.out.println("Not in an initialized Gitlet directory.");
            return;
        }

        List<String> files = plainFilenamesIn(Commit.COMMITS_FOLDER);
        for (String file : files) {
            readObject(join(Commit.COMMITS_FOLDER, file), Commit.class);
            commitFormatter(file);
        }
    }

    public static void find(String message) {
        if (!GITLET_DIR.exists()) {
            System.out.println("Not in an initialized Gitlet directory.");
            return;
        }

        List<String> files = plainFilenamesIn(Commit.COMMITS_FOLDER);
        boolean haveFile = false;
        for (String file : files) {
            Commit commit = readObject(join(Commit.COMMITS_FOLDER, file), Commit.class);
            if (commit.getMessage().equals(message)) {
                haveFile = true;
                System.out.println(file);
            }
        }
        if (!haveFile) {
            System.out.println("Found no commit with that message.");
        }
    }

    public static void status() {
        if (!GITLET_DIR.exists()) {
            System.out.println("Not in an initialized Gitlet directory.");
            return;
        }
        StringBuilder str = new StringBuilder();

        // 0. Print branches.
        str.append("=== Branches ===\n");
        List<String> branches = plainFilenamesIn(HEADS_FOLDER);
        branches.sort(String::compareTo);
        String head = getActiveBranch();
        str.append("*").append(head).append("\n");
        for (String branch : branches) {
            if (!branch.equals(head)) {
                str.append(branch).append("\n");
            }
        }

        // 1. Print staged files.
        str.append("\n").append("=== Staged Files ===\n");
        Stage stage = Stage.getStage();
        HashMap<String, String> toBeAdded = stage.getToBeAdded();
        ArrayList<String> addFiles = new ArrayList<>(toBeAdded.keySet());
        addFiles.sort(String::compareTo);
        for (String file : addFiles) {
            str.append(file).append("\n");
        }

        // 2. Print removed files.
        str.append("\n").append("=== Removed Files ===\n");
        LinkedList<String> removeFiles = stage.getToBeRemoved();
        removeFiles.sort(String::compareTo);
        for (String file : removeFiles) {
            str.append(file).append("\n");
        }

        // 3. Print modified files.
        str.append("\n").append("=== Modifications Not Staged For Commit ===\n");
        List<String> cwdFiles = plainFilenamesIn(CWD);
        ArrayList<String> modifiedFiles = new ArrayList<>();

        // 3.1 Staged for addition, but deleted or modified in CWD.
        for (String file : addFiles) {
            if (!cwdFiles.contains(file)) {
                modifiedFiles.add(file + " (deleted)");
            } else if (!sha1(readContents(new File(CWD, file))).equals(toBeAdded.get(file))) {
                modifiedFiles.add(file + " (modified)");
            }
        }
        // 3.2 Tracked in current commit, changed in CWD but not staged for addition;
        // 3.3 Tracked in current commit, deleted from CWD but not staged for removal.
        HashMap<String, String> committedFiles
                = Commit.readCommit(Commit.getCurrentCommitId()).getFiles();
        LinkedList<String> committedKeys = new LinkedList<>(committedFiles.keySet());
        for (String file : committedKeys) {
            if (cwdFiles.contains(file)
                    && !addFiles.contains(file)
                    && !sha1(readContents(new File(CWD, file))).equals(committedFiles.get(file))) {
                modifiedFiles.add(file + " (modified)");
            }
            if (!cwdFiles.contains(file) && !removeFiles.contains(file)) {
                modifiedFiles.add(file + " (deleted)");
            }
        }
        modifiedFiles.sort(String::compareTo);
        for (String file : modifiedFiles) {
            str.append(file).append("\n");
        }

        // 4. Print untracked files.
        str.append("\n").append("=== Untracked Files ===\n");
        for (String file : cwdFiles) {
            if (!addFiles.contains(file) && !committedFiles.containsKey(file)) {
                str.append(file).append("\n");
            }
        }
        System.out.println(str);
    }

    public static void checkoutFile(String filename) {
        if (!GITLET_DIR.exists()) {
            System.out.println("Not in an initialized Gitlet directory.");
            return;
        }

        String fileId = Commit.readCommit(Commit.getCurrentCommitId()).getFiles().get(filename);
        if (fileId == null) {
            System.out.println("File does not exist in that commit.");
            return;
        }

        String committedFileContent = readContentsAsString(join(OBJECTS_FOLDER, fileId));
        restrictedDelete(filename);
        createFile(filename, committedFileContent);
    }

    private static String getCommitId(String commitId) {
        List<String> commits = plainFilenamesIn(Commit.COMMITS_FOLDER);
        for (String commit : commits) {
            if (commit.contains(commitId)) {
                commitId = commit;
            }
        }
        return commitId;
    }

    private static boolean hasUntrackedFile() {
        List<String> cwdFiles = plainFilenamesIn(CWD);
        HashMap<String, String> currentCommittedFiles
                = Commit.readCommit(Commit.getCurrentCommitId()).getFiles();
        Set<String> addFiles = Stage.getStage().getToBeAdded().keySet();
        if (cwdFiles != null) {
            for (String file : cwdFiles) {
                if (!currentCommittedFiles.containsKey(file) && !addFiles.contains(file)) {
                    System.out.println("There is an untracked file in the way; "
                            + "delete it, or add and commit it first.");
                    return true;
                }
            }
        }
        return false;
    }

    private static void replaceFiles(String commitId) {
        HashMap<String, String> currentCommittedFiles
                = Commit.readCommit(Commit.getCurrentCommitId()).getFiles();
        HashMap<String, String> addFile = Stage.getStage().getToBeAdded();
        for (String file : currentCommittedFiles.keySet()) {
            restrictedDelete(file);
        }
        for (String file : addFile.keySet()) {
            restrictedDelete(file);
        }

        HashMap<String, String> targetCommittedFiles = Commit.readCommit(commitId).getFiles();
        for (String file : targetCommittedFiles.keySet()) {
            createFile(file,
                    readContentsAsString(join(OBJECTS_FOLDER, targetCommittedFiles.get(file))));
        }
    }

    public static void checkoutCommit(String commitId, String filename) {
        if (!GITLET_DIR.exists()) {
            System.out.println("Not in an initialized Gitlet directory.");
            return;
        }

        // Turn prefix to commit id.
        List<String> commits = plainFilenamesIn(Commit.COMMITS_FOLDER);
        if (commitId.length() < UID_LENGTH) {
            commitId = getCommitId(commitId);
        }

        // If no commit with the given id exists.
        if (!commits.contains(commitId)) {
            System.out.println("No commit with that id exists.");
            return;
        }

        // If the file does not exist in the given commit.
        String fileId = Commit.readCommit(commitId).getFiles().get(filename);
        if (fileId == null) {
            System.out.println("File does not exist in that commit.");
            return;
        }

        String committedFileContent = readContentsAsString(join(OBJECTS_FOLDER, fileId));
        restrictedDelete(filename);
        createFile(filename, committedFileContent);
    }

    public static void checkoutBranch(String branch) {
        if (!GITLET_DIR.exists()) {
            System.out.println("Not in an initialized Gitlet directory.");
            return;
        }

        // If no branch with that name exists.
        List<String> branches = plainFilenamesIn(HEADS_FOLDER);
        if (!branches.contains(branch)) {
            System.out.println("No such branch exists.");
            return;
        }

        // If that branch is the current branch.
        String currentBranch = getActiveBranch();
        if (currentBranch.equals(branch)) {
            System.out.println("No need to checkout the current branch.");
            return;
        }

        // If a working file is untracked and would be overwritten by the checkout.
        if (hasUntrackedFile()) {
            return;
        }

        // Replace files in CWD.
        String commitId = readContentsAsString(join(HEADS_FOLDER, branch));
        replaceFiles(commitId);

        // Set the given branch active.
        writeContents(join(GITLET_DIR, "HEAD"), join("refs", "heads", branch).toString());

        // Clear staging area.
        new Stage().persistStage();
    }

    public static void branch(String branch) {
        if (!GITLET_DIR.exists()) {
            System.out.println("Not in an initialized Gitlet directory.");
            return;
        }

        List<String> branches = plainFilenamesIn(HEADS_FOLDER);
        if (branches.contains(branch)) {
            System.out.println("A branch with that name already exists.");
            return;
        }

        String head = Commit.getCurrentCommitId();
        writeContents(join(HEADS_FOLDER, branch), head);
    }

    public static void rmBranch(String branch) {
        if (!GITLET_DIR.exists()) {
            System.out.println("Not in an initialized Gitlet directory.");
            return;
        }

        // If a branch with the given name does not exist.
        List<String> branches = plainFilenamesIn(HEADS_FOLDER);
        if (!branches.contains(branch)) {
            System.out.println("A branch with that name does not exist.");
            return;
        }

        // Try to remove the currently active branch.
        if (getActiveBranch().equals(branch)) {
            System.out.println("Cannot remove the current branch.");
            return;
        }

        join(HEADS_FOLDER, branch).delete();
    }

    public static void reset(String commitId) {
        if (!GITLET_DIR.exists()) {
            System.out.println("Not in an initialized Gitlet directory.");
            return;
        }

        // Turn prefix to commit id.
        List<String> commits = plainFilenamesIn(Commit.COMMITS_FOLDER);
        if (commitId.length() < UID_LENGTH) {
            commitId = getCommitId(commitId);
        }

        // If no commit with the given id exists.
        if (!commits.contains(commitId)) {
            System.out.println("No commit with that id exists.");
            return;
        }

        // If CWD has untracked files.
        if (hasUntrackedFile()) {
            return;
        }

        // Replace files in current commit with target commit.
        replaceFiles(commitId);

        // Move current branch's head to target commit.
        writeContents(join(HEADS_FOLDER, getActiveBranch()), commitId);

        // Clear staging area.
        new Stage().persistStage();
    }

    private static LinkedList<String> getAllCommitIds(String startingId) {
        LinkedList<String> givenBranchCommits = new LinkedList<>();
        givenBranchCommits.add(startingId);

        while (Commit.readCommit(startingId).getParents().size() > 0) {
            String parentId = Commit.readCommit(startingId).getParents().get(0);
            givenBranchCommits.add(parentId);
            startingId = parentId;
        }

        return givenBranchCommits;
    }

    public static void merge(String givenBranch) {
        if (!GITLET_DIR.exists()) {
            System.out.println("Not in an initialized Gitlet directory.");
            return;
        }

        // 0.1 Has untracked file in current commit.
        if (hasUntrackedFile()) {
            return;
        }
        // 0.2 If there are staged additions or removals present.
        Stage stage = Stage.getStage();
        if (stage.getToBeAdded().size() > 0 || stage.getToBeRemoved().size() > 0) {
            System.out.println("You have uncommitted changes.");
            return;
        }

        // 0.3 If a branch with the given name does not exist.
        if (!plainFilenamesIn(HEADS_FOLDER).contains(givenBranch)) {
            System.out.println("A branch with that name does not exist.");
            return;
        }

        // 0.4 If attempting to merge a branch with itself.
        String givenBranchId = readContentsAsString(join(HEADS_FOLDER, givenBranch));
        String currentBranch = getActiveBranch();
        LinkedList<String> givenBranchCommits = getAllCommitIds(givenBranchId);
        String currentBranchId = Commit.getCurrentCommitId();
        if (givenBranch.equals(currentBranch)) {
            System.out.println("Cannot merge a branch with itself.");
            return;
        }

        // 1. If the split point is the current branch.
        if (givenBranchCommits.contains(currentBranchId)) {
            checkoutBranch(givenBranch);
            System.out.println("Current branch fast-forwarded.");
            return;
        }

        // 2. Locate the split point.
        String splitPoint = getSplitPoint(currentBranchId, givenBranchCommits);

        // 3. If the split point is the same commit as the given branch.
        if (splitPoint.equals(givenBranchId)) {
            System.out.println("Given branch is an ancestor of the current branch.");
            return;
        }

        // 4. Merge files in cb, gb and sp
        mergeFiles(givenBranchId, currentBranchId, splitPoint);

        // 5. Make a commit.
        String message = "Merged " + givenBranch + " into " + currentBranch + ".";
        commit(message, givenBranchId);
    }

    private static String getSplitPoint(String commitId, LinkedList<String> givenBranchCommits) {
        LinkedList<String> marked = new LinkedList<>();
        LinkedList<String> fringe = Commit.readCommit(commitId).getParents();
        marked.add(commitId);
        marked.addAll(fringe);

        while (!fringe.isEmpty()) {
            String v = fringe.removeFirst();
            if (givenBranchCommits.contains(v)) {
                return v;
            }
            for (String parentId : Commit.readCommit(v).getParents()) {
                if (!marked.contains(parentId)) {
                    fringe.addLast(parentId);
                    marked.add(parentId);
                }
            }
        }
        return null;
    }

    private static void mergeFiles(String gbi, String cbi, String sp) {
        HashMap<String, String> gbf = Commit.readCommit(gbi).getFiles();
        HashMap<String, String> cbf = Commit.readCommit(cbi).getFiles();
        HashMap<String, String> spf = Commit.readCommit(sp).getFiles();
        HashSet<String> fileNames = new HashSet<>(gbf.keySet());
        fileNames.addAll(cbf.keySet());
        fileNames.addAll(spf.keySet());

        for (String f : fileNames) {
            // Absent in sp and gb, present in cb.
            if (!spf.containsKey(f) && !gbf.containsKey(f) && cbf.containsKey(f)) {
                continue;
            }
            // Absent in sp and cb, present in gb.
            if (!spf.containsKey(f) && !cbf.containsKey(f) && gbf.containsKey(f)) {
                checkoutCommit(gbi, f);
                add(f);
                continue;
            }
            // Removed in cb and gb, present in sp.
            if (!cbf.containsKey(f) && !gbf.containsKey(f)) {
                continue;
            }
            // Unmodified in cb, removed in gb.
            if (spf.get(f).equals(cbf.get(f)) && !gbf.containsKey(f)) {
                remove(f);
                continue;
            }
            // Unmodified in gb, removed in cb.
            if (spf.get(f).equals(gbf.get(f)) && !cbf.containsKey(f)) {
                continue;
            }
            // Unmodified in cb, modified in gb.
            if (spf.get(f).equals(cbf.get(f)) && !spf.get(f).equals(gbf.get(f))) {
                checkoutCommit(gbi, f);
                add(f);
                continue;
            }
            // Unmodified in gb, modified in cb.
            if (spf.get(f).equals(gbf.get(f)) && !spf.get(f).equals(cbf.get(f))) {
                continue;
            }
            // Modified in the same way in gb and cb, or unmodified.
            if (gbf.containsKey(f) && cbf.containsKey(f) && gbf.get(f).equals(cbf.get(f))) {
                continue;
            }
            // Modified in different ways in gb and cb.
            resolveConflict(cbf.get(f), gbf.get(f), f);
        }
    }

    private static void resolveConflict(String cbfi, String gbfi, String filename) {
        System.out.println("Encountered a merge conflict.");
        String cbf = "";
        String gbf = "";
        if (cbfi != null) {
            cbf = readContentsAsString(join(OBJECTS_FOLDER, cbfi));
        }
        if (gbfi != null) {
            gbf = readContentsAsString(join(OBJECTS_FOLDER, gbfi));
        }
        String fileContent = "<<<<<<< HEAD\n"
                + cbf
                + "=======\n"
                + gbf
                + ">>>>>>>\n";
        File filePath = new File(CWD, filename);
        writeContents(filePath, fileContent);
        add(filename);
    }
}
