package protect.card_locker.coverage;

/**
 * The CoverageTool class provides functionality to track which branches have been covered
 * during the execution of specific functions in the application. Each function has associated
 * boolean flags that indicate the coverage status for up to 100 possible branches.
 */
public class CoverageTool {

    private static boolean[] func1flags = new boolean[100];
    private static boolean[] func2flags = new boolean[100];
    private static boolean[] func3flags = new boolean[100];
    private static boolean[] func4flags = new boolean[100];
    private static boolean[] func5flags = new boolean[100];

    /**
     * Sets the flag for the specified branch that was taken in function 1.
     *
     * @param index the index of the branch that has been taken. Must be less than 100.
     */
    public static void setFunc1Flag(int index) {
        func1flags[index] = true;
    }

    /**
     * Sets the flag for the specified branch that was taken in function 2.
     *
     * @param index the index of the branch that has been taken. Must be less than 100.
     */
    public static void setFunc2Flag(int index) {
        func2flags[index] = true;
    }

    /**
     * Sets the flag for the specified branch that was taken in function 3.
     *
     * @param index the index of the branch that has been taken. Must be less than 100.
     */
    public static void setFunc3Flag(int index) {
        func3flags[index] = true;
    }

    /**
     * Sets the flag for the specified branch that was taken in function 4.
     *
     * @param index the index of the branch that has been taken. Must be less than 100.
     */
    public static void setFunc4Flag(int index) {
        func4flags[index] = true;
    }

    /**
     * Sets the flag for the specified branch that was taken in function 5.
     *
     * @param index the index of the branch that has been taken. Must be less than 100.
     */
    public static void setFunc5Flag(int index) {
        func5flags[index] = true;
    }

    /**
     * Outputs coverage statistics for the tracked functions. This method calculates and
     * prints the percentage of branch coverage for each each function.
     */
    public void outputCoverageStatistics() {
        // TODO: implement the output of the coverage statistics
    }
}
