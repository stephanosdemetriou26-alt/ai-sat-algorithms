import java.util.*;
import java.io.*;

public class BCSP_Project {
    static int N = 15;
    static int K = 4;
    static int PROBLEMS_PER_RATIO = 10;
    static int MAX_RESTARTS = 10;
    static long TIMEOUT_MS = 60000;

    public static void main(String[] args) {
        File dir = new File("my_problems");
        if (!dir.exists()) dir.mkdir();

        System.out.println("Ratio(M/N);Satisfiability(%);AvgTime_DFS(ms);AvgTime_Hill(ms)");

        for (double ratio = 2.0; ratio <= 12.0; ratio += 0.5) {
            int M = (int) (ratio * N);
            runRatioExperiment(ratio, M);
        }
    }

    static void runRatioExperiment(double ratio, int M) {
        int satCount = 0;
        List<Long> timesDFS = new ArrayList<>();
        List<Long> timesHill = new ArrayList<>();

        for (int p = 0; p < PROBLEMS_PER_RATIO; p++) {
            int[][] clauses = generateProblem(N, M, K);
            
            String fileName = "my_problems/prob_R" + ratio + "_id" + p + ".txt";
            saveProblemToFile(clauses, fileName);

            long startDFS = System.currentTimeMillis();
            boolean solvedDFS = solveDFS(clauses, new int[N], 0, startDFS);
            timesDFS.add(System.currentTimeMillis() - startDFS);

            long totalHillTime = 0;
            boolean solvedHillAny = false;
            for (int i = 0; i < 5; i++) {
                long startHill = System.currentTimeMillis();
                if (solveHillClimbing(clauses, startHill)) solvedHillAny = true;
                totalHillTime += (System.currentTimeMillis() - startHill);
            }
            timesHill.add(totalHillTime / 5);

            if (solvedDFS || solvedHillAny) satCount++;
        }

        double probSat = (double) satCount / PROBLEMS_PER_RATIO;
        double avgTimeDFS = timesDFS.stream().mapToLong(v -> v).average().orElse(0.0);
        double avgTimeHill = timesHill.stream().mapToLong(v -> v).average().orElse(0.0);
        System.out.printf(Locale.US, "%.1f;%.2f;%.2f;%.2f%n", ratio, probSat, avgTimeDFS, avgTimeHill);
    }

    static void saveProblemToFile(int[][] clauses, String filename) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
            writer.println(N + " " + clauses.length + " " + K);
            for (int[] clause : clauses) {
                for (int i = 0; i < K; i++) {
                    writer.print(clause[i] + (i == K - 1 ? "" : " "));
                }
                writer.println();
            }
        } catch (IOException e) { e.printStackTrace(); }
    }

    static int[][] generateProblem(int n, int m, int k) {
        int[][] clauses = new int[m][k];
        Random rand = new Random();
        for (int i = 0; i < m; i++) {
            Set<Integer> usedVars = new HashSet<>();
            for (int j = 0; j < k; j++) {
                int var;
                do { var = rand.nextInt(n) + 1; } while (usedVars.contains(var));
                usedVars.add(var);
                if (rand.nextBoolean()) var = -var;
                clauses[i][j] = var;
            }
        }
        return clauses;
    }

    static boolean solveDFS(int[][] clauses, int[] assignment, int varIndex, long startTime) {
        if (System.currentTimeMillis() - startTime > TIMEOUT_MS) return false;
        if (!isValid(clauses, assignment)) return false;
        if (varIndex == N) return true;
        assignment[varIndex] = -1;
        if (solveDFS(clauses, assignment, varIndex + 1, startTime)) return true;
        assignment[varIndex] = 1;
        if (solveDFS(clauses, assignment, varIndex + 1, startTime)) return true;
        assignment[varIndex] = 0;
        return false;
    }

    static boolean isValid(int[][] clauses, int[] assignment) {
        for (int[] clause : clauses) {
            boolean clauseIsFalse = true;
            boolean clauseIsUnfinished = false;
            for (int literal : clause) {
                int varIndex = Math.abs(literal) - 1;
                int val = assignment[varIndex];
                if (val == 0) { clauseIsUnfinished = true; clauseIsFalse = false; break; }
                if ((literal > 0 && val == 1) || (literal < 0 && val == -1)) { clauseIsFalse = false; break; }
            }
            if (!clauseIsUnfinished && clauseIsFalse) return false;
        }
        return true;
    }

    static boolean solveHillClimbing(int[][] clauses, long startTime) {
        Random rand = new Random();
        int[] assignment = new int[N];
        for (int restart = 0; restart < MAX_RESTARTS; restart++) {
            for (int i = 0; i < N; i++) assignment[i] = rand.nextBoolean() ? 1 : -1;
            int currentUnsatisfied = countUnsatisfied(clauses, assignment);
            while (currentUnsatisfied > 0) {
                if (System.currentTimeMillis() - startTime > TIMEOUT_MS) return false;
                int bestFlip = -1;
                int bestUnsatisfied = currentUnsatisfied;
                for (int i = 0; i < N; i++) {
                    assignment[i] = -assignment[i];
                    int unsat = countUnsatisfied(clauses, assignment);
                    if (unsat < bestUnsatisfied) { bestUnsatisfied = unsat; bestFlip = i; }
                    assignment[i] = -assignment[i];
                }
                if (bestFlip != -1) {
                    assignment[bestFlip] = -assignment[bestFlip];
                    currentUnsatisfied = bestUnsatisfied;
                } else break;
            }
            if (currentUnsatisfied == 0) return true;
        }
        return false;
    }

    static int countUnsatisfied(int[][] clauses, int[] assignment) {
        int unsatisfied = 0;
        for (int[] clause : clauses) {
            boolean satisfied = false;
            for (int literal : clause) {
                int varIndex = Math.abs(literal) - 1;
                if ((literal > 0 && assignment[varIndex] == 1) || (literal < 0 && assignment[varIndex] == -1)) {
                    satisfied = true; break;
                }
            }
            if (!satisfied) unsatisfied++;
        }
        return unsatisfied;
    }
}