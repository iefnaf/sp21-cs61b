package gitlet;

/** Driver class for Gitlet, a subset of the Git version-control system.
 *  @author gfanfei@gmail.com
 */
public class Main {

    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND1> <OPERAND2> ... 
     */
    public static void main(String[] args) {
        if (args.length == 0) {
            Utils.exitWithError("Please enter a command.");
        }
        String firstArg = args[0];
        if (!firstArg.equals("init") && !gitletSetupTest()) {
            Utils.exitWithError("Not in an initialized Gitlet directory.");
        }

        switch (firstArg) {
            case "init" -> {
                validateNumArgs(args, 1);
                Repository.handleInit();
            }
            case "add" -> {
                validateNumArgs(args, 2);
                Repository.handleAdd(args[1]);
            }
            case "commit" -> {
                validateNumArgs(args, 2);
                Repository.handleCommit(args[1]);
            }
            case "checkout" -> Repository.handleCheckout(args);
            case "log" -> {
                validateNumArgs(args, 1);
                Repository.handleLog();
            }
            case "global-log" -> {
                validateNumArgs(args, 1);
                Repository.handleGlobalLog();
            }
            case "find" -> {
                validateNumArgs(args, 2);
                Repository.handleFind(args[1]);
            }
            case "status" -> {
                validateNumArgs(args, 1);
                Repository.handleStatus();
            }
            case "rm" -> {
                validateNumArgs(args, 2);
                Repository.handleRm(args[1]);
            }
            case "branch" -> {
                validateNumArgs(args, 2);
                Repository.handleBranch(args[1]);
            }
            case "rm-branch" -> {
                validateNumArgs(args, 2);
                Repository.handleRmBranch(args[1]);
            }
            case "reset" -> {
                validateNumArgs(args, 2);
                Repository.handleReset(args[1]);
            }
            case "merge" -> {
                validateNumArgs(args, 2);
                Repository.handleMerge(args[1]);
            }
            default -> Utils.exitWithError("No command with that name exists.");
        }
    }

    public static void validateNumArgs(String[] args, int n) {
        if (args.length != n) {
            Utils.exitWithError("Incorrect operands.");
        }
    }

    private static boolean gitletSetupTest() {
        return Repository.GITLET_DIR.exists();
    }
}
