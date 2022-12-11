package gitlet;
import static gitlet.Utils.*;

/** Driver class for Gitlet, a subset of the Git version-control system.
 *  @author TODO
 */
public class Main {

    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND1> <OPERAND2> ... 
     */
    public static void main(String[] args) {
        // TODO: what if args is empty?
        if (args.length == 0) {
            System.out.println("Please enter a command.");
            System.exit(0);
        }
        String firstArg = args[0];
        switch(firstArg) {
            case "init":
                // TODO: handle the `init` command
                validateArgNum(args, 1);
                Repository.init();
                break;
            case "add":
                // TODO: handle the `add [filename]` command
                validateArgNum(args, 2);
                Repository.add(args[1]);
                break;
            case "commit":
                if (args.length == 1) {
                    System.out.println("Please enter a commit message.");
                    System.exit(0);
                }
                validateArgNum(args, 2);
                Repository.commit(args[1]);
                break;
            case "rm":
                validateArgNum(args, 2);
                Repository.rm(args[1]);
                break;
            case "log":
                validateArgNum(args, 1);
                Repository.log();
                break;
            case "global-log":
                validateArgNum(args, 1);
                Repository.logGlob();
                break;
            case "find":
                validateArgNum(args, 2);
                Repository.find(args[1]);
                break;
            case "status":
                validateArgNum(args, 1);
                Repository.status();
                break;
            case "checkout":
                if (args.length == 2) {
                    Repository.checkoutBranch(args[1]);
                } else if (args.length == 3) {
                    if (!args[1].equals("--")) {
                        printExit("Incorrect operands");
                    }
                    Repository.checkoutFile(args[2]);

                } else if (args.length == 4) {
                    if (!args[2].equals("--")) {
                        printExit("Incorrect operands");
                    }
                    Repository.checkoutCommitFile(args[1], args[3]);
                } else {
                    printExit("Incorrect operands");
                }
                break;

            default:
                System.out.println("No command with that name exists.");
                System.exit(0);
                break;
            // TODO: FILL THE REST IN
        }
    }
}
