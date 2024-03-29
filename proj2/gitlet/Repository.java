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
        if (!setupPersistence()) {
            exitWithError("Failed to create Gitlet related directories.");
        }

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
        Map<String, String> headM = getHeadMap();
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
        String head = getHeadCommitId();
        Map<String, String> map = getHeadMap();
        HashMap<String, String> addition = readAddition();
        HashSet<String> removal = readRemoval();

        if (addition.isEmpty() && removal.isEmpty()) {
            exitWithError("No changes added to the commit.");
        }

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
        assert (tracked != null);
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
            assert (headC != null);
            head = headC.getFirstParent();
        }
        System.out.print(sb);
    }

    /** Displays information about all commits ever made. */
    public static void handleGlobalLog() {
        StringBuilder sb = new StringBuilder();
        List<String> commitIds = plainFilenamesIn(Commit.COMMIT_FOLDER);
        assert(commitIds != null);
        for (String commitId : commitIds) {
            sb.append(logForOneCommit(commitId));
        }

        System.out.print(sb);
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
            assert (commit != null);
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

        // print files that were modified but not staged
        sb.append("=== Modifications Not Staged For Commit ===\n");
        Set<String> modifiedNotStagedFiles = getModifiedNotStagedFiles();
        for (String file : modifiedNotStagedFiles) {
            sb.append(file).append(" (modified)").append("\n");
        }
        Set<String> deletedNotStagedFiles = getDeletedNotStagedFiles();
        for (String file : deletedNotStagedFiles) {
            sb.append(file).append(" (deleted)").append("\n");
        }
        sb.append("\n");

        // print untracked files
        sb.append("=== Untracked Files ===\n");
        Set<String> untrackedFiles = getUntrackedFiles();
        for (String file : untrackedFiles) {
            sb.append(file).append("\n");
        }
        sb.append("\n");

        System.out.print(sb);
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
                if (!branchExists(targetBranch)) {
                    exitWithError("No such branch exists.");
                }

                String currentBranch = getCurrentBranch();
                if (targetBranch.equals(currentBranch)) {
                    exitWithError("No need to checkout the current branch.");
                }

                String currentCommitId = getHeadCommitId();
                String targetCommitId = readBranch(targetBranch);
                handleReset(targetCommitId);

                // handleReset will write targetCommitId in current branch,
                // but we should not modify it.
                writeBranch(currentBranch, currentCommitId);

                // don't forget to modify current branch
                setCurrentBranch(targetBranch);
            }
            case 3 -> {
                // checkout -- [file name]
                if (!args[1].equals("--")) {
                    exitWithError("Incorrect operands.");
                }
                String fileName = args[2];
                Map<String, String> headM = getHeadMap();
                if (!headM.containsKey(fileName)) {
                    exitWithError("File does not exist in that commit.");
                }

                // read the file in head commit, write it in CWD.
                String blobName = headM.get(fileName);
                overwriteWorkingFile(fileName, blobName);
            }
            case 4 -> {
                // checkout [commit id] -- [file name]
                if (!args[2].equals("--")) {
                    exitWithError("Incorrect operands.");
                }
                String commitId = args[1];
                String fileName = args[3];
                if (commitId.length() < UID_LENGTH) {
                    commitId = sid2lid(commitId);
                }
                if (!Commit.commitExists(commitId)) {
                    exitWithError("No commit with that id exists.");
                }

                Map<String, String> map = getTrackedMap(commitId);
                assert (map != null);
                if (!map.containsKey(fileName)) {
                    exitWithError("File does not exist in that commit.");
                }

                // overwrite
                String blobName = map.get(fileName);
                overwriteWorkingFile(fileName, blobName);
            }
            default -> exitWithError("Incorrect operands.");
        }
    }

    /**
     * Checks out all the files tracked by the given commit.
     * Removes tracked files that are not present in that commit.
     * Also moves the current branch’s head to that commit node.
     * The staging area is cleared.
     */
    public static void handleReset(String targetCommitId) {
        if (targetCommitId == null) {
            throw new NullPointerException();
        }
        if (targetCommitId.length() < UID_LENGTH) {
            targetCommitId = sid2lid(targetCommitId);
        }
        if (!Commit.commitExists(targetCommitId)) {
            exitWithError("No commit with that id exists.");
        }

        // If a working file is untracked in the current branch and
        // is tracked in the target commit,
        // it would be overwritten by the checkout.
        Set<String> untrackedFiles = getUntrackedFiles();
        Set<String> targetTrackedFiles = getTrackedFiles(targetCommitId);
        assert (targetTrackedFiles != null);
        for (String file : untrackedFiles) {
            if (targetTrackedFiles.contains(file)) {
                exitWithError("There is an untracked file in the way; " +
                        "delete it, or add and commit it first.");
            }
        }

        // delete files exists in target commit but not exists in current commit.
        deleteUnnecessaryFiles(targetCommitId);

        // overwrite files
        Map<String, String> targetM = getTrackedMap(targetCommitId);
        assert (targetM != null);
        targetM.forEach(Repository::overwriteWorkingFile);

        // clear staging area
        clearStagingArea();

        // moves the current branch’s head to that commit node
        String currentB = getCurrentBranch();
        writeBranch(currentB, targetCommitId);
    }

    /** Merge other branch to current branch. */
    public static void handleMerge(String otherB) {
        HashMap<String, String> stagedAddition = readAddition();
        HashSet<String> stagedRemoval = readRemoval();
        if (!stagedAddition.isEmpty() || !stagedRemoval.isEmpty()) {
            exitWithError("You have uncommitted changes.");
        }
        if (!branchExists(otherB)) {
            exitWithError("A branch with that name does not exist.");
        }
        String currB = getCurrentBranch();
        if (currB.equals(otherB)) {
            exitWithError("Cannot merge a branch with itself.");
        }
        String splitCommitId = findSplitPoint(otherB);
        assert (splitCommitId != null);
        String otherCommitId = readBranch(otherB);
        if (splitCommitId.equals(otherCommitId)) {
            exitWithMessage("Given branch is an ancestor of the current branch.");
        }
        String currCommitId = getHeadCommitId();
        if (splitCommitId.equals(currCommitId)) {
            String[] args = {"checkout", otherB};
            handleCheckout(args);
            exitWithMessage("Current branch fast-forwarded.");
        }

        Set<String> currTrackedFiles = getTrackedFiles(currCommitId);
        Set<String> splitTrackedFiles = getTrackedFiles(splitCommitId);
        Set<String> otherTrackedFiles = getTrackedFiles(otherCommitId);
        Set<String> currRemovedFiles = getRemovedFiles(currTrackedFiles, splitTrackedFiles);
        Set<String> otherRemovedFiles = getRemovedFiles(otherTrackedFiles, splitTrackedFiles);
        Set<String> currAddedFiles = getAddedFiles(currTrackedFiles, splitTrackedFiles);
        Set<String> otherAddedFiles = getAddedFiles(otherTrackedFiles, splitTrackedFiles);
        Map<String, String> currMap = getTrackedMap(currCommitId);
        assert (currMap != null);
        Map<String, String> splitMap = getTrackedMap(splitCommitId);
        assert (splitMap != null);
        Map<String, String> otherMap = getTrackedMap(otherCommitId);
        assert (otherMap != null);
        Set<String> currModifiedFiles = getModifiedFiles(currMap, splitMap);
        Set<String> otherModifiedFiles = getModifiedFiles(otherMap, splitMap);

        // Any files modified in different ways in the current and given branches are in conflict.
        Set<String> filesToRemove = new HashSet<>();
        Set<String> filesToAdd = new HashSet<>();
        Set<String> conflictFiles = new HashSet<>();

        for (String file : otherRemovedFiles) {
            if (currModifiedFiles.contains(file)) {
                conflictFiles.add(file);
            } else {
                if (!currRemovedFiles.contains(file)) {
                    filesToRemove.add(file);
                }
            }
        }

        for (String file : otherAddedFiles) {
            if (!currAddedFiles.contains(file)) {
                filesToAdd.add(file);
            } else {
                if (!currMap.get(file).equals(otherMap.get(file))) {
                    conflictFiles.add(file);
                }
            }
        }

        for (String file : otherModifiedFiles) {
            if (currRemovedFiles.contains(file)) {
                conflictFiles.add(file);
            } else {
                if (!currModifiedFiles.contains(file)) {
                    filesToAdd.add(file);
                } else {
                    if (!currMap.get(file).equals(otherMap.get(file))) {
                        conflictFiles.add(file);
                    }
                }
            }
        }

        Set<String> untrackedFiles = getUntrackedFiles();
        for (String file : untrackedFiles) {
            if (filesToRemove.contains(file) || filesToAdd.contains(file) ||
                    conflictFiles.contains(file)) {
                exitWithError("There is an untracked file in the way; " +
                        "delete it, or add and commit it first.");
            }
        }

        HashSet<String> removal = readRemoval();
        HashMap<String, String> addition = readAddition();

        for (String fileName : filesToRemove) {
            deleteWorkingFile(fileName);
            addition.remove(fileName);
            removal.add(fileName);
        }

        for (String fileName : filesToAdd) {
            String blobName = otherMap.get(fileName);
            overwriteWorkingFile(fileName, blobName);
            addition.put(fileName, blobName);
            removal.remove(fileName);
        }

        if (!conflictFiles.isEmpty()) {
            System.out.println("Encountered a merge conflict.");
            for (String fileName : conflictFiles) {
                StringBuilder sb = new StringBuilder();
                sb.append("<<<<<<< HEAD\n");
                if (currTrackedFiles.contains(fileName)) {
                    String currBlobName = currMap.get(fileName);
                    String currContents = readBlob(currBlobName);
                    sb.append(currContents);
                }
                sb.append("=======\n");
                if (otherTrackedFiles.contains(fileName)) {
                    String otherBlobName = otherMap.get(fileName);
                    String otherContents = readBlob(otherBlobName);
                    sb.append(otherContents);
                }
                sb.append(">>>>>>>\n");
                String newContents = sb.toString();
                writeWorkingFile(fileName, newContents);
                String blobName = sha1(newContents);
                writeBlob(blobName, newContents);
                addition.put(fileName, blobName);
                removal.remove(fileName);
            }
        }

        String message = String.format("Merged %s into %s.", otherB, currB);
        currMap.putAll(addition);
        removal.forEach(currMap::remove);
        Commit newC = new Commit(message, new Date(System.currentTimeMillis()), currCommitId, otherCommitId, currMap);
        String newHead = Commit.saveCommit(newC);
        writeBranch(currB, newHead);
        clearStagingArea();
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
        if (branch == null || branch.isBlank()) {
            return false;
        }
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

    /** Return all files in the working directory. */
    private static Set<String> getWorkingFiles() {
        Set<String> workingFiles = new HashSet<>();
        List<String> workingFilesList = plainFilenamesIn(CWD);
        if (workingFilesList == null) {
            return workingFiles;
        }
        workingFiles.addAll(workingFilesList);
        return workingFiles;
    }

    /** Return all files and its hash in the working directory. */
    private static Map<String, String> getWorkingMap() {
        Set<String> workingFiles = getWorkingFiles();
        HashMap<String, String> workingMap = new HashMap<>();
        for (String fileName : workingFiles) {
            String contents = readWorkingFile(fileName);
            String hash = sha1(contents);
            workingMap.put(fileName, hash);
        }
        return workingMap;
    }

    // ==================== other helper functions ====================

    /** Create gitlet related directories. */
    private static boolean setupPersistence() {
        boolean res = GITLET_DIR.mkdir();
        res &= BLOBS_FOLDER.mkdir();
        res &= Commit.COMMIT_FOLDER.mkdir();
        res &= STAGING_FOLDER.mkdir();
        res &= BRANCHES_FOLDER.mkdir();
        clearStagingArea();
        return res;
    }

    /** Get the corresponding commit id represent by the shorted id. */
    private static String sid2lid(String sid) {
        List<String> commits = plainFilenamesIn(Commit.COMMIT_FOLDER);
        assert (commits != null);
        for (String commit : commits) {
            if (commit.startsWith(sid)) {
                return commit;
            }
        }
        return null;
    }

    /**
     * Helper function for handleCheckout.
     * Use contents in blob to overwrite file in working directory(CWD).
     */
    private static void overwriteWorkingFile(String fileName, String blobName) {
        if (!blobExists(blobName)) {
            return;
        }

        String contents = readBlob(blobName);
        writeWorkingFile(fileName, contents);
    }

    /** Return files that are tracked by the specified commit. */
    private static Set<String> getTrackedFiles(String commitId) {
        if (!Commit.commitExists(commitId)) {
            return null;
        }

        Map<String, String> map = getTrackedMap(commitId);
        assert (map != null);
        return map.keySet();
    }

    /** Return the map of the specified commit. */
    private static Map<String, String> getTrackedMap(String commitId) {
        if (!Commit.commitExists(commitId)) {
            return null;
        }

        Commit commit = Commit.fromFile(commitId);
        assert (commit != null);
        return commit.getMap();
    }

    /** Return the map of the head commit. */
    private static Map<String, String> getHeadMap() {
        String head = getHeadCommitId();
        return getTrackedMap(head);
    }

    /**
     * Helper function for status command.
     * A file in the working directory is “modified but not staged” if it is:
     * 1. Tracked in the current commit, changed in the working directory, but not staged for addition
     * 2. Staged for addition, but with different contents than in the working directory
     */
    private static Set<String> getModifiedNotStagedFiles() {
        Set<String> modified = new HashSet<>();
        Map<String, String> currentMap = getHeadMap();
        Map<String, String> workingMap = getWorkingMap();
        Map<String, String> additionMap = readAddition();

        for (String fileName : workingMap.keySet()) {
            String hash = workingMap.get(fileName);
            if (additionMap.containsKey(fileName)) {
                if (!additionMap.get(fileName).equals(hash)) {
                    modified.add(fileName);
                }
            } else {
                if (currentMap.containsKey(fileName) && !currentMap.get(fileName).equals(hash)) {
                    modified.add(fileName);
                }
            }
        }

        return modified;
    }

    /**
     * Helper function for status command.
     * A file in the working directory is “deleted but not staged” if it is:
     * 1. Tracked in the current commit, deleted in the working directory, but not staged for removal
     * 2. Staged for addition, but deleted in the working directory
     */
    private static Set<String> getDeletedNotStagedFiles() {
        Set<String> deleted = new HashSet<>();

        String head = getHeadCommitId();
        Set<String> trackedSet = getTrackedFiles(head);
        assert (trackedSet != null);
        Set<String> workingSet = getWorkingFiles();
        Set<String> additionSet = getStagedFiles();
        Set<String> removalSet = readRemoval();

        for (String fileName : trackedSet) {
            if (!workingSet.contains(fileName) && !removalSet.contains(fileName)) {
                deleted.add(fileName);
            }
        }

        for (String fileName : additionSet) {
            if (!workingSet.contains(fileName)) {
                deleted.add(fileName);
            }
        }
        return deleted;
    }

    /**
     * Helper function for status.
     * Return untracked files.
     * Untracked Files is files present in the working directory
     * but neither staged for addition nor tracked.
     */
    private static Set<String> getUntrackedFiles() {
        Set<String> addition = readAddition().keySet();
        String headCommitId = getHeadCommitId();
        Set<String> trackedFiles = getTrackedFiles(headCommitId);
        assert (trackedFiles != null);
        Set<String> untrackedFiles = new HashSet<>();
        Set<String> workingFiles = getWorkingFiles();
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
        if (!Commit.commitExists(commitId)) {
            return null;
        }

        StringBuilder sb = new StringBuilder();
        Formatter formatter = new Formatter(sb);
        SimpleDateFormat sdf = new SimpleDateFormat("EEE MMM d HH:mm:ss yyyy Z", Locale.ENGLISH);
        Commit commit = Commit.fromFile(commitId);
        assert (commit != null);

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
        assert (currentFiles != null);
        Set<String> targetFiles = getTrackedFiles(targetCommitId);
        assert (targetFiles != null);
        currentFiles.removeAll(targetFiles);
        for (String file : currentFiles) {
            deleteWorkingFile(file);
        }
    }

    /**
     * Helper function for merge.
     * Return all the previous commits' id of the given commit (include itself).
     */
    private static Set<String> getAncestors(String head) {
        if (!Commit.commitExists(head)) {
            throw new GitletException("No such commit exists");
        }
        Set<String> ans = new HashSet<>();
        Queue<String> queue = new ArrayDeque<>();
        queue.add(head);
        while (!queue.isEmpty()) {
            int size = queue.size();
            for (int i = 0; i < size; i++) {
                String commitId = queue.remove();
                ans.add(commitId);
                Commit commit = Commit.fromFile(commitId);
                assert (commit != null);
                String firstParent = commit.getFirstParent();
                if (firstParent != null) {
                    queue.add(firstParent);
                }
                String secondParent = commit.getSecondParent();
                if (secondParent != null) {
                    queue.add(secondParent);
                }
            }
        }
        return ans;
    }

    /**
     * Helper function for merge.
     * Find the split point of the current branch and the given branch.
     * The split point is the latest common ancestor of the current and given branch heads.
     */
    private static String findSplitPoint(String otherB) {
        String currCommitId = getHeadCommitId();
        String otherCommitId = readBranch(otherB);
        Set<String> otherAncestors = getAncestors(otherCommitId);
        if (otherAncestors.contains(currCommitId)) {
            return currCommitId;
        }
        Set<String> currAncestors = getAncestors(currCommitId);
        if (currAncestors.contains(otherCommitId)) {
            return otherCommitId;
        }

        Queue<String> queue = new ArrayDeque<>();
        queue.add(currCommitId);
        while (!queue.isEmpty()) {
            int size = queue.size();
            for (int i = 0; i < size; i++) {
                String commitId = queue.remove();
                if (otherAncestors.contains(commitId)) {
                    return commitId;
                }
                Commit c = Commit.fromFile(commitId);
                assert (c != null);
                String firstParent = c.getFirstParent();
                if (firstParent != null) {
                    queue.add(firstParent);
                }
                String secondParent = c.getSecondParent();
                if (secondParent != null) {
                    queue.add(secondParent);
                }
            }
        }
        return null;
    }

    /**
     * Helper function for merge.
     * Return all files that are modified.
     */
    private static Set<String> getModifiedFiles(Map<String, String> currM, Map<String, String> prevM) {
        Set<String> result = new HashSet<>();
        for (String file : currM.keySet()) {
            if (prevM.containsKey(file) && !prevM.get(file).equals(currM.get(file))) {
                result.add(file);
            }
        }
        return result;
    }

    /**
     * Helper function for merge.
     * Return all files that are added.
     */
    private static Set<String> getAddedFiles(Set<String> currS, Set<String> prevS) {
        Set<String> result = new HashSet<>(currS);
        result.removeAll(prevS);
        return result;
    }

    /**
     * Helper function for merge.
     * Return all files that are removed.
     */
    private static Set<String> getRemovedFiles(Set<String> currS, Set<String> prevS) {
        Set<String> result = new HashSet<>(prevS);
        result.removeAll(currS);
        return result;
    }
}
