package gitlet;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;

import static gitlet.Utils.*;
import static gitlet.Utils.exitWithError;

/** Represents a gitlet repository.
 *
 *  .gitlet/ -- top level folder for all persistent data
 *      - blobs/ -- stores all the blobs(user's files)
 *      - commits/ -- stores all the commits
 *      - stagingArea/ -- staging area
 *          - ADDITION -- staged for addition, a hash map fileName->fileHash(version)
 *          - REMOVAL -- staged for removal, a hash set contains filename for removal
 *      - branches/ -- stores all the branch file.
 *                  file's name is branch's name, file's contents is the latest commit hash of the branch.
 *      - HEAD -- stores the name of current branch.
 *
 *  @author gfanfei@gmail.com
 */
public class Repository {
    public static final File CWD = new File(System.getProperty("user.dir"));
    public static final File GITLET_DIR = join(CWD, ".gitlet");
    public static final File BLOBS_FOLDER = join(GITLET_DIR, "blobs");
    public static final File STAGING_FOLDER = join(GITLET_DIR, "stagingArea");
    public static final File BRANCHES_FOLDER = join(GITLET_DIR, "branches");
    public static final File HEAD = join(GITLET_DIR, "HEAD");
    public static final File ADDITION = join(STAGING_FOLDER, "ADDITION");
    public static final File REMOVAL = join(STAGING_FOLDER, "REMOVAL");

    /**
     * Creates a new Gitlet version-control system in the current directory.
     */
    public static void handleInit() {
        // step1. check if .gitlet exists
        if (GITLET_DIR.exists()) {
            exitWithError("A Gitlet version-control system already exists in the current directory.");
        }

        // step2. create related directories
        setupPersistence();

        // step3. create init commit and master branch, set HEAD point to master.
        Commit initC = new Commit("initial commit", new Date(0), null, null, new HashMap<>());
        String initCommitId = Commit.saveCommit(initC);
        writeBranch("master", initCommitId);
        setCurrentBranch("master");
    }

    /**
     * Adds a copy of the file as it currently exists to the staging area.
     */
    public static void handleAdd(String fileName) {
        // step1. If file does not exist, print error message and exist.
        if (!workingFileExists(fileName)) {
            exitWithError("File does not exist.");
        }

        // step2. Get the hashmap of the latest commit and data structure of Staging area.
        Commit headC = getHeadCommit();
        HashMap<String, String> headM = headC.getMap();
        HashMap<String, String> addition = readAddition();
        HashSet<String> removal = readRemoval();

        // step3. Remove from Staged for removal
        removal.remove(fileName);

        // step4. If this file is identical to the latest commit,
        // remove it from Staging for addition.
        String contents = readWorkingFile(fileName);
        String blobName = sha1(contents);
        if (headM.get(fileName) != null && headM.get(fileName).equals(blobName)) {
            addition.remove(fileName);
        } else {
            // Create a blob and add it to Staging for addition.
            writeBlob(blobName, contents);
            addition.put(fileName, blobName);
        }

        // step5. update staging area
        writeAddition(addition);
        writeRemoval(removal);
    }

    /**
     * Saves a snapshot of tracked files in the current commit(head commit) and staging area,
     * so they can be restored at a later time.
     */
    public static void handleCommit(String message) {
        if (message.isBlank()) {
            exitWithError("Please enter a commit message.");
        }

        String currentB = getCurrentBranch();
        String head = readBranch(currentB);
        Commit headC = Commit.fromFile(head);
        HashMap<String, String> map = headC.getMap();
        HashMap<String, String> addition = readAddition();
        HashSet<String> removal = readRemoval();

        map.putAll(addition);
        removal.forEach(map::remove);
        Commit newC = new Commit(message, new Date(System.currentTimeMillis()), head, null, map);
        String newHead = Commit.saveCommit(newC);
        writeBranch(currentB, newHead);

        clearStagingArea();
    }

    /** Unstage the file if it is currently staged for addition. */
    public static void handleRm(String fileName) {
        if (fileName.isBlank()) {
            exitWithError("Please enter a file name.");
        }

        // step1. Get the tracked files and data structure of Staging area.
        String head = getHeadCommitId();
        Set<String> tracked = getTrackedFiles(head);
        HashMap<String, String> addition = readAddition();
        HashSet<String> removal = readRemoval();

        // step2. If the file is neither staged nor tracked by the head commit,
        // print error message.
        if (!tracked.contains(fileName) && !addition.containsKey(fileName)) {
            exitWithError("No reason to remove the file.");
        }

        // step3. unstage the file
        addition.remove(fileName);

        // step4. If the file is tracked in the current commit,
        // stage it for removal and remove it from working directory.
        if (tracked.contains(fileName)) {
            removal.add(fileName);
            deleteWorkingFile(fileName);
        }

        // step6. Update Staging area
        writeAddition(addition);
        writeRemoval(removal);
    }

    /**
     * Starting at the current head commit,
     * display information about each commit backwards along the commit tree
     * until the initial commit, following the first parent commit links.
     */
    public static void handleLog() {
        StringBuilder sb = new StringBuilder();
        String head = getHeadCommitId();
        while (head != null && !head.isBlank()) {
            sb.append(logForOneCommit(head));
            Commit headC = Commit.fromFile(head);
            head = headC.getFirstParent();
        }
        System.out.println(sb);
    }

    /** Displays information about all commits ever made. */
    public static void handleGlobalLog() {
        StringBuilder sb = new StringBuilder();
        List<String> commitIds = plainFilenamesIn(Commit.COMMIT_FOLDER);
        assert(commitIds != null);
        for (String commitId : commitIds) {
            sb.append(logForOneCommit(commitId));
        }

        System.out.println(sb);
    }

    /** Prints out the ids of all commits that have the given commit message. */
    public static void handleFind(String commitMessage) {
        if (commitMessage.isBlank()) {
            Utils.exitWithError("Please enter a commit message.");
        }

        StringBuilder sb = new StringBuilder();
        List<String> commitIds = plainFilenamesIn(Commit.COMMIT_FOLDER);
        assert(commitIds != null);
        for (String commitId : commitIds) {
            Commit commit = Commit.fromFile(commitId);
            if (commit.getMessage().equals(commitMessage)) {
                sb.append(commitId).append("\n");
            }
        }
        if (sb.length() != 0) {
            System.out.println(sb);
        } else {
            System.out.println("Found no commit with that message.");
        }
    }

    /**
     * Displays what branches currently exist,
     * and marks the current branch with a *.
     * Also displays what files have been staged for addition or removal.
     */
    public static void handleStatus() {
        StringBuilder sb = new StringBuilder();

        // print all Branches
        sb.append("=== Branches ===\n");
        String currentB = getCurrentBranch();
        sb.append("*").append(currentB).append("\n");
        List<String> branches = plainFilenamesIn(BRANCHES_FOLDER);
        assert(branches != null);
        for (String branch : branches) {
            if (!branch.equals(currentB)) {
                sb.append(branch).append("\n");
            }
        }
        sb.append("\n");

        // print Staged Files
        sb.append("=== Staged Files ===\n");
        Set<String> stagedFiles = getStagedFiles();
        for (String file : stagedFiles) {
            sb.append(file).append("\n");
        }
        sb.append("\n");

        // print Removed Files
        sb.append("=== Removed Files ===\n");
        Set<String> removedFiles = getRemovedFiles();
        for (String file : removedFiles) {
            sb.append(file).append("\n");
        }
        sb.append("\n");

        // todo: Modifications Not Staged For Commit

        // todo: Untracked Files

        System.out.println(sb);
    }

    /**
     * Creates a new branch with the given name,
     * and points it at the current head commit.
     */
    public static void handleBranch(String branchName) {
        if (branchExists(branchName)) {
            exitWithError("A branch with that name already exists.");
        }

        String head = getHeadCommitId();
        writeBranch(branchName, head);
    }

    /**
     * Deletes the branch with the given name.
     * This only means to delete the pointer associated with the branch;
     * it does not mean to delete all commits that were created under the branch,
     * or anything like that.
     */
    public static void handleRmBranch(String branchName) {
        String currentB = getCurrentBranch();
        if (currentB.equals(branchName)) {
            exitWithError("Cannot remove the current branch.");
        }

        if (!branchExists(branchName)) {
            exitWithError("A branch with that name does not exist.");
        }

        File branchF = join(BRANCHES_FOLDER, branchName);
        branchF.delete();
    }

    /**
     * Checkout is a kind of general command that
     * can do a few different things depending on what its arguments are.
     *
     * Usages:
     *  1. checkout -- [file name]
     *     Takes the version of the file as it exists in the head commit and puts it in the working directory.
     *
     *  2. checkout [commit id] -- [file name]
     *     Takes the version of the file as it exists in the commit with the given id,
     *     and puts it in the working directory.
     *
     *  3. checkout [branch name]
     *     Takes all files in the commit at the head of the given branch, and puts them in the working directory.
     *     Also, at the end of this command, the given branch will now be considered the current branch (HEAD).
     *     Any files that are tracked in the current branch but are not present in the checked-out branch are deleted.
     *     The staging area is cleared.
     */
    public static void handleCheckout(String[] args) {
        switch(args.length) {
            case 2 -> {
                // checkout [branch name]
                String targetBranch = args[1];
                String currentBranch = getCurrentBranch();
                if (targetBranch.equals(currentBranch)) {
                    exitWithError("No need to checkout the current branch.");
                }

                String targetCommitId = readBranch(targetBranch);
                handleReset(targetCommitId);

                // don't forget to modify HEAD
                setCurrentBranch(targetBranch);
            }
            case 3 -> {
                // checkout -- [file name]
                String fileName = args[2];
                Commit headC = getHeadCommit();
                HashMap<String, String> headM = headC.getMap();
                if (!headM.containsKey(fileName)) {
                    exitWithError("File does not exist in that commit.");
                }

                // read the file in head commit, write it in CWD.
                String blobName = headM.get(fileName);
                overwriteFile(fileName, blobName);
            }
            case 4 -> {
                // checkout [commit id] -- [file name]
                String commitId = args[1];
                String fileName = args[3];
                Commit commit = Commit.fromFile(commitId);
                if (commit == null) {
                    exitWithError("No commit with that id exists.");
                }
                HashMap<String, String> map = commit.getMap();
                if (!map.containsKey(fileName)) {
                    exitWithError("File does not exist in that commit.");
                }

                // overwrite
                String blobName = map.get(fileName);
                overwriteFile(fileName, blobName);
            }
            default -> {
                exitWithError("Incorrect operands.");
            }
        }
    }

    /**
     * Checks out all the files tracked by the given commit.
     * Removes tracked files that are not present in that commit.
     * Also moves the current branch’s head to that commit node.
     * The staging area is cleared.
     */
    public static void handleReset(String targetCommitId) {
        File commitF = join(Commit.COMMIT_FOLDER, targetCommitId);
        if (!commitF.exists()) {
            exitWithError("No commit with that id exists.");
        }

        Set<String> untrackedFiles = getUntrackedFiles();
        if (!untrackedFiles.isEmpty()) {
            exitWithError("There is an untracked file in the way; delete it, or add and commit it first.");
        }

        // delete files exists in target commit but not exists in current commit.
        deleteUnnecessaryFiles(targetCommitId);

        // overwrite files
        Commit targetC = Commit.fromFile(targetCommitId);
        Map<String, String> targetM = targetC.getMap();
        targetM.forEach(Repository::overwriteFile);

        // clear staging area
        clearStagingArea();

        // don't forget to modify HEAD
        setCurrentBranch(targetCommitId);
    }

    public static void handleMerge(String branchName) {

    }

    // ==================== commit helper functions ====================

    /** Return the commit id of the current commit(head commit). */
    private static String getHeadCommitId() {
        String branch = getCurrentBranch();
        return readBranch(branch);
    }

    /** Return the current commit(head commit) object. */
    private static Commit getHeadCommit() {
        String commitId = getHeadCommitId();
        return Commit.fromFile(commitId);
    }

    // ==================== blob helper functions ====================

    /** Return whether the specified blob exists. */
    private static boolean blobExists(String blobName) {
        File blobF = join(BLOBS_FOLDER, blobName);
        return blobF.exists();
    }

    /** Return contents of the specified blob. */
    private static String readBlob(String blobName) {
        if (!blobExists(blobName)) {
            return null;
        }
        File blobF = join(BLOBS_FOLDER, blobName);
        return readContentsAsString(blobF);
    }

    /** Write contents to the specified blob. */
    private static void writeBlob(String blobName, String contents) {
        File blobF = join(BLOBS_FOLDER, blobName);
        writeContents(blobF, contents);
    }

    // ==================== branch helper functions ====================

    /** Return whether the specified branch exists. */
    private static boolean branchExists(String branch) {
        File branchF = join(BRANCHES_FOLDER, branch);
        return branchF.exists();
    }

    /** Return the name of current branch. */
    private static String getCurrentBranch() {
        return readContentsAsString(HEAD);
    }

    /** Set current branch. */
    private static void setCurrentBranch(String branch) {
        writeContents(HEAD, branch);
    }

    /** Return head commits' id of the specified branch. */
    private static String readBranch(String branch) {
        if (!branchExists(branch)) {
            return null;
        }
        File branchF = join(BRANCHES_FOLDER, branch);
        return readContentsAsString(branchF);
    }

    /** Update the head commit of the specified branch. */
    private static void writeBranch(String branch, String commitId) {
        File branchF = join(BRANCHES_FOLDER, branch);
        writeContents(branchF, commitId);
    }

    // ==================== staging area helper functions ====================

    /** Return the map stored at Staged for addition area. */
    private static HashMap<String, String> readAddition() {
        return readObject(ADDITION, HashMap.class);
    }

    /** Write the map to Staged for addition area. */
    private static void writeAddition(HashMap<String, String> add) {
        writeObject(ADDITION, add);
    }

    /** Return the set stored at Staged for removal area. */
    private static HashSet<String> readRemoval() {
        return readObject(REMOVAL, HashSet.class);
    }

    /** Write the set to Staged for removal area. */
    private static void writeRemoval(HashSet<String> removal) {
        writeObject(REMOVAL, removal);
    }

    /** Clear the staging area. */
    private static void clearStagingArea() {
        writeAddition(new HashMap<>());
        writeRemoval(new HashSet<>());
    }

    /** Return staged files(files that staged for addition). */
    private static Set<String> getStagedFiles() {
        HashMap<String, String> addMap = readAddition();
        return addMap.keySet();
    }

    /** Return removed files(files that staged for removal). */
    private static Set<String> getRemovedFiles() {
        return readRemoval();
    }

    // ==================== working area helper functions ====================

    /** Return whether the specified file exists in working directory. */
    private static boolean workingFileExists(String fileName) {
        File file = join(CWD, fileName);
        return file.exists();
    }

    /** Return contents of the specified file in working directory. */
    private static String readWorkingFile(String fileName) {
        if (!workingFileExists(fileName)) {
            return null;
        }
        File file = join(CWD, fileName);
        return readContentsAsString(file);
    }

    /** Write contents to the specified file in working directory. */
    private static void writeWorkingFile(String fileName, String contents) {
        File file = join(CWD, fileName);
        writeContents(file, contents);
    }

    /** Delete the specified file in working directory. */
    private static void deleteWorkingFile(String fileName) {
        if (!workingFileExists(fileName)) {
            return;
        }
        File file = join(CWD, fileName);
        file.delete();
    }

    // ==================== other helper functions ====================

    /** Create gitlet related directories. */
    private static void setupPersistence() {
        GITLET_DIR.mkdir();
        BLOBS_FOLDER.mkdir();
        Commit.COMMIT_FOLDER.mkdir();
        STAGING_FOLDER.mkdir();
        BRANCHES_FOLDER.mkdir();
        clearStagingArea();
    }

    /**
     * Helper function for handleCheckout.
     * Use contents in blob to overwrite file in working directory(CWD).
     */
    private static void overwriteFile(String fileName, String blobName) {
        if (!blobExists(blobName)) {
            return;
        }

        String contents = readBlob(blobName);
        writeWorkingFile(fileName, contents);
    }

    /** Return files that are tracked by the specified commit. */
    private static Set<String> getTrackedFiles(String commitId) {
        if (commitId == null || commitId.isBlank() || !Commit.commitExists(commitId)) {
            return null;
        }

        Commit commit = Commit.fromFile(commitId);
        HashMap<String, String> map = commit.getMap();
        return map.keySet();
    }

    /**
     * A file in the working directory is “modified but not staged” if it is
     * 1. Tracked in the current commit, changed in the working directory, but not staged
     * 2. Staged for addition, but with different contents than in the working directory
     * 3. Staged for addition, but deleted in the working directory
     * 4. Not staged for removal, but tracked in the current commit and deleted from the working directory
     */
    private static Set<String> getModifiedNotStagedFiles() {
        // todo
        Set<String> files = new HashSet<>();
        return files;
    }

    /**
     * Return untracked files.
     * Untracked Files is files present in the working directory
     * but neither staged for addition nor tracked.
     */
    private static Set<String> getUntrackedFiles() {
        Set<String> addition = readAddition().keySet();
        String headCommitId = getHeadCommitId();
        Set<String> trackedFiles = getTrackedFiles(headCommitId);
        Set<String> untrackedFiles = new HashSet<>();
        List<String> workingFiles = plainFilenamesIn(CWD);
        assert(workingFiles != null);
        for (String file : workingFiles) {
            if (!addition.contains(file) && !trackedFiles.contains(file)) {
                untrackedFiles.add(file);
            }
        }
        return untrackedFiles;
    }

    /**
     * Helper function for log and global-log.
     * @return a formatted log for the specified commit.
     */
    private static String logForOneCommit(String commitId) {
        if (commitId.isBlank() || !Commit.commitExists(commitId)) {
            return null;
        }

        StringBuilder sb = new StringBuilder();
        Formatter formatter = new Formatter(sb);
        SimpleDateFormat sdf = new SimpleDateFormat("E M d HH:mm:ss yyyy Z", Locale.ENGLISH);
        Commit commit = Commit.fromFile(commitId);

        sb.append("===\n");
        // print commit + hash
        formatter.format("commit %s\n", commitId);

        // if merged from two commits, print Merge + hash of parents
        String firstParent = commit.getFirstParent();
        String secondParent = commit.getSecondParent();
        if (secondParent != null) {
            formatter.format("Merge: %s %s\n", firstParent.substring(0, 7), secondParent.substring(0, 7));
        }

        // print formatted date
        Date date = commit.getDate();
        String fDate = sdf.format(date);
        formatter.format("Date: %s\n", fDate);

        // print message
        formatter.format("%s\n", commit.getMessage());

        // print a new line
        sb.append("\n");

        return sb.toString();
    }

    /**
     * Helper function for handleCheckout and handleReset.
     * Delete files that are tracked in the current commit but are not present in the specified commit.
     */
    private static void deleteUnnecessaryFiles(String targetCommitId) {
        String currentCommitId = getHeadCommitId();
        Set<String> currentFiles = getTrackedFiles(currentCommitId);
        Set<String> targetFiles = getTrackedFiles(targetCommitId);
        currentFiles.removeAll(targetFiles);
        for (String file : currentFiles) {
            deleteWorkingFile(file);
        }
    }
}
