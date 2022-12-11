package gitlet;



import java.io.File;
import java.util.*;

import static gitlet.Utils.*;

// TODO: any imports you need here

/** Represents a gitlet repository.
 *  TODO: It's a good idea to give a description here of what else this Class
 *  does at a high level.
 *
 *  @author Quanzhi
 */
public class Repository {
    /**
     * TODO: add instance variables here.
     *
     * List all instance variables of the Repository class here with a useful
     * comment above them describing what that variable represents and how that
     * variable is used. We've provided two examples for you.
     */

    /** The current working directory. */
    public static final File CWD = new File(System.getProperty("user.dir"));
    /** The .gitlet directory. */
    public static final File GITLET_DIR = join(CWD, ".gitlet");
    public static final File OBJECTS_DIR = join(GITLET_DIR, "objects");
    public static final File COMMITS_DIR = join(GITLET_DIR, "commits");
    public static final File STAGED_DIR = join(GITLET_DIR, "stagedObj");

    /* TODO: fill in the rest of this class. */
    // validate if the repo initialized
    private static void validateRepoExisted() {
        if (!GITLET_DIR.exists()) {
            System.out.println("Not in an initialized Gitlet directory.");
            System.exit(0);
        }
    }
    // get staged map from the file system
    private static HashMap<String, String> getStaged() {
        File stagedPath = join(GITLET_DIR, "STAGED");
        HashMap<String, String> staged = readObject(stagedPath, (new HashMap<String, String>()).getClass());
        return staged;
    }
    // get branches
    private static HashMap<String, String> getBranch() {
        File branchesPath = join(GITLET_DIR, "BRANCHES");
        HashMap<String, String> branch = readObject(branchesPath, (new HashMap<String, String>()).getClass());
        return branch;
    }

    private static void printCommit(String commitHash, Commit commit) {

        System.out.println("===");
        System.out.println("commit " + commitHash);
        if (commit.secondParent != null) {
            System.out.println("Merge: " + commit.parent.substring(0, 7) + " " + commit.secondParent.substring(0, 7));
        }
        System.out.println(String.format("Date: %1$ta %1$tb %1$tT %1$tY %1$tz", commit.date));
        System.out.println(commit.message);
        System.out.println();
        return;
    }
    // get a commit object by its name
    private static Commit getCommit(String commitName) {
        return readObject(join(COMMITS_DIR, commitName), (new Commit()).getClass());
    }
    // get the head pointer
    private static String getHead() {
        return readContentsAsString(join(GITLET_DIR, "HEAD")).split("\n")[0];
    }
    // get head branch
    private static String getHeadBranch() {
        return readContentsAsString(join(GITLET_DIR, "HEAD")).split("\n")[1];
    }
    // print elements of a String list line by line
    private static void printList(List<String> lst) {
        for (String s : lst) {
            System.out.println(s);
        }
    }
    // search for commit that start with commit Name
    private static ArrayList<String> searchCommitName(String commitName) {
        ArrayList<String> res = new ArrayList<>();
        for (String f : plainFilenamesIn(COMMITS_DIR)) {
            int idx = f.indexOf(commitName);
            if (idx == 0) {
                res.add(f);
            }
        }
        return res;
    }

    /**
     * initialize .git repo to save informations
     */
    public static void init() {
        // create repo structures
        if (!GITLET_DIR.exists()) {
            GITLET_DIR.mkdir();
            OBJECTS_DIR.mkdir();
            COMMITS_DIR.mkdir();
            STAGED_DIR.mkdir();
        } else {
            System.err.println("A Gitlet version-control system already exists in the current directory.");
            System.exit(0);
        }
        // create the initial commit
        Date start = new Date(0);
        Commit init = new Commit("initial commit", null, start);
        try {
            String commitName = sha1(serialize(init));
            File init_commit = join(COMMITS_DIR, commitName);
            init_commit.createNewFile();
            writeObject(init_commit, init);
            // create head pointer
            File head = join(GITLET_DIR, "HEAD");
            head.createNewFile();
            // point head pointer to the initial Commit
            writeContents(head, commitName + "\nmaster");
            // initialize staged field
            HashMap<String, String> staged = new HashMap<>();
            File staged_file = join(GITLET_DIR, "STAGED");
            staged_file.createNewFile();
            writeObject(staged_file, staged);
            // initialize branches
            HashMap<String, String> branches = new HashMap<>();
            // set master to initial commit
            branches.put("master", commitName);
            File branch_file = join(GITLET_DIR, "BRANCHES");
            writeObject(branch_file, branches);
        } catch (Exception e) {
            System.err.println(e);
        }
    }

    public static void add(String file) {
        // validate if there is an initialized repo
        validateRepoExisted();

        // process file to be added
        File toAdd = join(CWD, file);
        byte[] fileContent =  readContents(toAdd);
        // compute sha-1 for the toAdd file
        String fileSha1 = sha1(fileContent);


        // get staged area
        HashMap<String, String> staged = getStaged();
        // get current commit
        String head = getHead();
        Commit currCommit = getCommit(head);

        // check if the version is same as current commit
        // if not same/not in in current commit, add to staged
        if (currCommit.files.get(file) == null || !fileSha1.equals(currCommit.files.get(file))) {
            // store the file into objects directory
            try {
                // if another version of file already in the staged area, replace it
                if (staged.get(file) != null && !staged.get(file).equals(fileSha1)) {
                    join(STAGED_DIR, staged.get(file)).delete();
//                    ///////////////////////////// FOR DEBUGGING PURPOSE//////////////////////////////////
//                    System.out.println(staged.get(file).equals(fileSha1));
//                    System.out.println(fileSha1);
//                    System.out.println(staged.get(file) + " are deleted");
                }
                File toAddInObj = join(STAGED_DIR, fileSha1);
                toAddInObj.createNewFile();
                writeContents(toAddInObj, fileContent);
                staged.put(file, fileSha1); // add file to be added to staged area
//                ///////////////////////////// FOR DEBUGGING PURPOSE//////////////////////////////////
//                System.out.println("file " + file + " added, the sha1 is " + fileSha1);
            } catch (Exception e) {
                System.err.println(e);
            }

            // else remove from the staged area / do not add
        } else {
            // if not in staged area, remove from stages and delete the corresponding file
            if (staged.get(file) != null) {
                File needDelete = join(STAGED_DIR, staged.get(file));
                needDelete.delete();
                staged.remove(file);
            }

        }
        // write staged back to STAGED file
        writeObject(join(GITLET_DIR, "STAGED"), staged);
    }

    public static void commit(String msg) {
        // validate if there is an initialized repo
        validateRepoExisted();

        // check if the staged area is empty
        HashMap<String, String> staged = readObject(join(GITLET_DIR, "STAGED"), (new HashMap<String, String>()).getClass());
        if (staged.size() == 0) {
            System.out.println("No changes added to the commit.");
            System.exit(0);
        }

        // create a new commit
        String prevHead = getHead();
        Commit prevCommit = getCommit(prevHead);
        Commit newCommit = new Commit(prevCommit);
        newCommit.parent = prevHead;
        newCommit.message = msg;

        // copy staged file into the commit
        for (Map.Entry<String, String> entry : staged.entrySet()) {
            // if the entry is NULL, indicate that file deleted, remove pair from the commit
            if (entry.getValue() == null) {
                newCommit.files.remove(entry.getKey());
                continue;
            }
            // put name sha1 pair into commit
            newCommit.files.put(entry.getKey(), entry.getValue());
            // copy files into objects dir
            File copiedFile = join(OBJECTS_DIR, entry.getValue());
            try {
                copiedFile.createNewFile();
            } catch (Exception e) {
                System.err.println(e);
                System.exit(0);
            }
            File stagedFile = join(STAGED_DIR, entry.getValue());
            byte[] stagedContent = readContents(stagedFile);
            writeContents(copiedFile, stagedContent);
            // delete corresponding file in stagedObj
            stagedFile.delete();
        }
        // empty the staged Map and save back
        staged.clear();
        writeObject(join(GITLET_DIR, "STAGED"), staged);
        // save the new commit
        String newSha1 = sha1(serialize(newCommit));
        writeObject(join(COMMITS_DIR, newSha1), newCommit);
        // update head and branch
        String currentBranch = getHeadBranch();
        HashMap<String, String> branchMap = getBranch();
        writeContents(join(GITLET_DIR, "HEAD"), newSha1 + "\n" + currentBranch);
        branchMap.put(currentBranch, newSha1);
        writeObject(join(GITLET_DIR, "BRANCHES"), branchMap);

        return;
    }

    public static void rm(String rmFile) {
        // validate if the repo existed
        validateRepoExisted();

        // read staged and current commit
        HashMap<String, String> staged = getStaged();
        String head = getHead();
        Commit currentCommit = getCommit(head);
        // if the file is staged, remove from staged and delete the corresponding file
        if (staged.get(rmFile) != null) {
            File fileToRemove = join(STAGED_DIR, staged.get(rmFile));
            fileToRemove.delete();
            staged.remove(rmFile);
        }
        // if the file is in the current commit, remove it from dir and mark it as removal in staged map
        if (currentCommit.files.get(rmFile) != null) {
            // remove from current dir
            if (join(CWD, rmFile).exists()) {
                restrictedDelete(join(CWD, rmFile));
            }
            // marked as removed and save staged
            staged.put(rmFile, null);

        }
        // save staged
        writeObject(join(GITLET_DIR, "STAGED"), staged);
        return;
    }
    // print log out
    public static void log() {
        // validate if repo exist
        validateRepoExisted();
        // get current head and commit
        String nextPtr = getHead();

        // print commit information
        do {
            Commit commitPtr = getCommit(nextPtr);
            printCommit(nextPtr, commitPtr);
            nextPtr = commitPtr.parent;
        } while (nextPtr != null);
        return;
    }
    // print global log
    public static void logGlob() {
        validateRepoExisted();
        List<String> commitList = plainFilenamesIn(COMMITS_DIR);
        for (String commitName : commitList) {
            Commit curr = getCommit(commitName);
            printCommit(commitName, curr);
        }
        return;
    }
    // print ids with given commits messages
    public static void find(String msg) {
        validateRepoExisted();
        List<String> commitList = plainFilenamesIn(COMMITS_DIR);
        boolean existed = false;
        // iterate through all commits in the dir
        for (String commitName : commitList) {
            Commit curr = getCommit(commitName);
            if (curr.message.equals(msg)) {
                System.out.println(commitName);
                existed = true;
            }
        }
        if (!existed) {
            System.out.println("Found no commit with that message.");
        }
        return;
    }
    // print out current status
    public static void status() {
        List<String> branchList = new ArrayList<>();
        List<String> removedList = new ArrayList<>();
        List<String> stagedList = new ArrayList<>();
        List<String> untrackedList = new ArrayList<>();
        List<String> modList = new ArrayList<>();
        validateRepoExisted();
        // get staged and current commit
        HashMap<String, String> staged = getStaged();
        String head = getHead();
        Commit curr = getCommit(head);
        // prepare branches List
        HashMap<String, String> branches = getBranch();
        for (String b : branches.keySet()) {
            branchList.add(b);
        }

        // prepare staged and removeList
        for (String file : staged.keySet()) {
            if (staged.get(file) != null) {
                stagedList.add(file);
            } else {
                removedList.add(file);
            }
        }
        // prepare modList
        for (String file : curr.files.keySet()) {
            if (!join(CWD, file).exists()) {
                modList.add(file + "(delete)");
            } else {
                byte[] fileContent = readContents(join(CWD, file));
                String fileSha1 = sha1(fileContent);
                if (!curr.files.get(file).equals(fileSha1)) {
                    if (staged.get(file) == null || !staged.get(file).equals(fileSha1)){
                        modList.add(file + "(modified)");
                    }
                }
            }
        }
        // prepare untrackedList
        for (String file : plainFilenamesIn(CWD)) {
            if (!curr.files.containsKey(file) && !staged.containsKey(file)) {
                untrackedList.add(file);
            }
        }
        Collections.sort(branchList);
        Collections.sort(removedList);
        Collections.sort(modList);
        Collections.sort(stagedList);
        Collections.sort(untrackedList);
        // print branches
        System.out.println("=== Branches ===");
        String headBranch = getHeadBranch();
        for (String b : branchList) {
            if (b.equals(headBranch)) {
                b = "*" + b;
            }
            System.out.println(b);
        }
        System.out.println();

        // print staged file
        System.out.println("=== Staged Files ===");
        printList(stagedList);
        System.out.println();
        // print removed file
        System.out.println("=== Removed Files ===");
        printList(removedList);
        System.out.println();
        // print modification not staged for commit
        System.out.println("=== Modifications Not Staged for Commit ===");
        printList(modList);
        System.out.println();
        // print untracked file (not in staged and current commit)
        System.out.println("=== Untracked Files ===");
        printList(untrackedList);
        System.out.println();
        return;
    }
    public static void checkoutBranch(String bName) {
        return;
    }

    public static void checkoutFile(String fName) {
        String head = getHead();
        Commit commit = getCommit(head);
        if (!commit.files.containsKey(fName)) {
            printExit("File does not exist in that commit.");
        }
        try {
            byte[] cont = readContents(join(OBJECTS_DIR, commit.files.get(fName)));
            File newFile = join(CWD, fName);
            if (!newFile.exists()) {
                newFile.createNewFile();
            }
            writeContents(newFile, cont);
        } catch (Exception e) {
            System.err.println(e);
            System.exit(0);
        }
        return;
    }

    public static void checkoutCommitFile(String commitName, String fName) {
        // search for commitName
        String head = getHead();
        Commit commit = getCommit(head);
        if (commitName.length() <= 40) {
            ArrayList<String> commitFull = searchCommitName(commitName);
            if (commitFull.size() == 1) {
                commit = getCommit(commitFull.get(0));
            } else if (commitFull.size() == 0) {
                System.out.println("No commit with that id exists.");
                System.exit(1);
            } else {
                System.out.println("The commit id ambiguous");
                System.exit(1);
            }
        } else if (commitName.length() == 160 && join(COMMITS_DIR, commitName).exists()) {
            commit = getCommit(commitName);
        } else {
            System.out.println("No commit with that id exists.");
            System.exit(1);
        }
        String fileName = commit.files.get(fName);
        if (fileName == null) {
            System.out.println("File does not exist in that commit.");
            System.exit(0);
        }
        byte[] fileContent = readContents(join(OBJECTS_DIR, fileName));
        File writeFile = join(CWD, fName);
        try {
            if (!writeFile.exists()) {
                writeFile.createNewFile();
            }
            writeContents(writeFile, fileContent);
        } catch (Exception e) {
            System.err.println(e);
            System.exit(0);
        }
        return;
    }
}
