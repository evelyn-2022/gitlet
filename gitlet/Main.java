package gitlet;

/** Driver class for Gitlet, a subset of the Git version-control system.
 *  @author Evelyn
 */
public class Main {

    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND1> <OPERAND2> ... 
     */
    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Please enter a command.");
            System.exit(0);
        }

        String firstArg = args[0];
        switch (firstArg) {
            case "init" -> {
                if (args.length == 1) {
                    Repository.init();
                    break;
                }
                System.out.println("Incorrect operands.");
                System.exit(0);
            }
            case "add" -> {
                if (args.length == 2) {
                    Repository.add(args[1]);
                    break;
                }
                System.out.println("Incorrect operands.");
                System.exit(0);
            }
            case "commit" -> {
                if (args.length == 1) {
                    System.out.println("Please enter a commit message.");
                    System.exit(0);
                }
                if (args.length == 2) {
                    Repository.commit(args[1], null);
                    break;
                }
                System.out.println("Incorrect operands.");
                System.exit(0);
            }
            case "rm" -> {
                if (args.length == 2) {
                    Repository.remove(args[1]);
                    break;
                }
                System.out.println("Incorrect operands.");
                System.exit(0);
            }
            case "log" -> {
                if (args.length == 1) {
                    Repository.log();
                    break;
                }
                System.out.println("Incorrect operands.");
                System.exit(0);
            }
            case "global-log" -> {
                if (args.length == 1) {
                    Repository.globalLog();
                    break;
                }
                System.out.println("Incorrect operands.");
                System.exit(0);
            }
            case "find" -> {
                if (args.length == 2) {
                    Repository.find(args[1]);
                    break;
                }
                System.out.println("Incorrect operands.");
                System.exit(0);
            }
            case "status" -> {
                if (args.length == 1) {
                    Repository.status();
                    break;
                }
                System.out.println("Incorrect operands.");
                System.exit(0);
            }
            case "checkout" -> {
                if (args.length == 3 && args[1].equals("--")) {
                    Repository.checkoutFile(args[2]);
                    break;
                }
                if (args.length == 4 && args[2].equals("--")) {
                    Repository.checkoutCommit(args[1], args[3]);
                    break;
                }
                if (args.length == 2) {
                    Repository.checkoutBranch(args[1]);
                    break;
                }
                System.out.println("Incorrect operands.");
                System.exit(0);
            }
            case "branch" -> {
                if (args.length == 2) {
                    Repository.branch(args[1]);
                    break;
                }
                System.out.println("Incorrect operands.");
                System.exit(0);
            }
            case "rm-branch" -> {
                if (args.length == 2) {
                    Repository.rmBranch(args[1]);
                    break;
                }
                System.out.println("Incorrect operands.");
                System.exit(0);
            }
            case "reset" -> {
                if (args.length == 2) {
                    Repository.reset(args[1]);
                    break;
                }
                System.out.println("Incorrect operands.");
                System.exit(0);
            }
            case "merge" -> {
                if (args.length == 2) {
                    Repository.merge(args[1]);
                    break;
                }
                System.out.println("Incorrect operands.");
                System.exit(0);
            }
            default -> {
                System.out.println("No command with that name exists.");
                System.exit(0);
            }
        }
    }
}
