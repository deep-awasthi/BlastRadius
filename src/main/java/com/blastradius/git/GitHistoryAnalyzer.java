package com.blastradius.git;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Analyzes git history of a local repository using CLI process execution.
 * Extracts contributor stats, bus factor, frequently changed files, and co-change pairs.
 */
@Component
public class GitHistoryAnalyzer {

    private static final Logger log = LoggerFactory.getLogger(GitHistoryAnalyzer.class);
    private static final int MAX_LOG_LINES = 5000;

    /**
     * Full git intelligence report for a repository path.
     */
    public GitReport analyze(String repoPath) {
        GitReport report = new GitReport();
        report.setRepoPath(repoPath);

        File repoDir = new File(repoPath);
        if (!repoDir.exists() || !isGitRepository(repoDir)) {
            log.warn("Not a git repository: {}", repoPath);
            report.setError("Not a git repository or .git folder not found");
            return report;
        }

        try {
            report.setContributors(extractContributors(repoDir));
            report.setFrequentlyChangedFiles(extractFrequentlyChangedFiles(repoDir));
            report.setTotalCommits(countTotalCommits(repoDir));
            report.setBusFactor(computeBusFactor(report.getContributors(), report.getTotalCommits()));
            report.setFilesChangedTogether(extractCoChangedFiles(repoDir));
        } catch (Exception e) {
            log.error("Git analysis failed for {}: {}", repoPath, e.getMessage());
            report.setError("Git analysis error: " + e.getMessage());
        }

        return report;
    }

    private boolean isGitRepository(File dir) {
        return new File(dir, ".git").exists();
    }

    private List<ContributorStat> extractContributors(File repoDir) throws Exception {
        List<String> lines = runGit(repoDir, "git", "shortlog", "-sn", "--all");
        List<ContributorStat> stats = new ArrayList<>();
        for (String line : lines) {
            line = line.trim();
            if (line.isBlank()) continue;
            String[] parts = line.split("\\s+", 2);
            if (parts.length == 2) {
                try {
                    int commits = Integer.parseInt(parts[0].trim());
                    String name = parts[1].trim();
                    stats.add(new ContributorStat(name, commits));
                } catch (NumberFormatException ignored) {}
            }
        }
        return stats;
    }

    private List<FileChangeStat> extractFrequentlyChangedFiles(File repoDir) throws Exception {
        List<String> lines = runGit(repoDir,
                "git", "log", "--pretty=format:", "--name-only", "--no-merges");

        Map<String, Integer> freq = new LinkedHashMap<>();
        for (String line : lines) {
            String trimmed = line.trim();
            if (!trimmed.isBlank() && trimmed.endsWith(".java")) {
                freq.merge(trimmed, 1, Integer::sum);
            }
        }

        return freq.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(20)
                .map(e -> new FileChangeStat(e.getKey(), e.getValue()))
                .collect(Collectors.toList());
    }

    private int countTotalCommits(File repoDir) throws Exception {
        List<String> lines = runGit(repoDir, "git", "rev-list", "--count", "HEAD");
        if (!lines.isEmpty()) {
            try { return Integer.parseInt(lines.get(0).trim()); } catch (NumberFormatException ignored) {}
        }
        return 0;
    }

    private int computeBusFactor(List<ContributorStat> contributors, int totalCommits) {
        if (contributors.isEmpty() || totalCommits == 0) return 0;

        int cumulative = 0;
        int busFactor = 0;
        double threshold = totalCommits * 0.5; // 50% of all commits

        for (ContributorStat c : contributors) {
            cumulative += c.commits();
            busFactor++;
            if (cumulative >= threshold) break;
        }
        return busFactor;
    }

    private List<CoChangeStat> extractCoChangedFiles(File repoDir) throws Exception {
        List<String> lines = runGit(repoDir,
                "git", "log", "--pretty=format:COMMIT", "--name-only", "--no-merges");

        // Group files by commit
        Map<String, Integer> pairCount = new LinkedHashMap<>();
        List<String> currentCommitFiles = new ArrayList<>();

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.equals("COMMIT")) {
                // Process previous commit's files
                buildPairs(currentCommitFiles, pairCount);
                currentCommitFiles.clear();
            } else if (!trimmed.isBlank() && trimmed.endsWith(".java")) {
                currentCommitFiles.add(trimmed);
            }
        }
        buildPairs(currentCommitFiles, pairCount);

        return pairCount.entrySet().stream()
                .filter(e -> e.getValue() > 1)
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(10)
                .map(e -> {
                    String[] parts = e.getKey().split("\\|", 2);
                    return new CoChangeStat(
                            parts.length > 0 ? parts[0] : "",
                            parts.length > 1 ? parts[1] : "",
                            e.getValue());
                })
                .collect(Collectors.toList());
    }

    private void buildPairs(List<String> files, Map<String, Integer> pairCount) {
        for (int i = 0; i < files.size(); i++) {
            for (int j = i + 1; j < files.size(); j++) {
                String key = files.get(i) + "|" + files.get(j);
                pairCount.merge(key, 1, Integer::sum);
            }
        }
    }

    private List<String> runGit(File workDir, String... command) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(workDir);
        pb.redirectErrorStream(true);

        Process process = pb.start();
        List<String> output = new ArrayList<>();
        int lineCount = 0;

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null && lineCount < MAX_LOG_LINES) {
                output.add(line);
                lineCount++;
            }
        }

        process.waitFor();
        return output;
    }

    // --- Data Records ---

    public record ContributorStat(String name, int commits) {}
    public record FileChangeStat(String filePath, int changeCount) {}
    public record CoChangeStat(String fileA, String fileB, int coChangeCount) {}

    public static class GitReport {
        private String repoPath;
        private int totalCommits;
        private int busFactor;
        private List<ContributorStat> contributors = new ArrayList<>();
        private List<FileChangeStat> frequentlyChangedFiles = new ArrayList<>();
        private List<CoChangeStat> filesChangedTogether = new ArrayList<>();
        private String error;

        public String getRepoPath() { return repoPath; }
        public void setRepoPath(String repoPath) { this.repoPath = repoPath; }
        public int getTotalCommits() { return totalCommits; }
        public void setTotalCommits(int totalCommits) { this.totalCommits = totalCommits; }
        public int getBusFactor() { return busFactor; }
        public void setBusFactor(int busFactor) { this.busFactor = busFactor; }
        public List<ContributorStat> getContributors() { return contributors; }
        public void setContributors(List<ContributorStat> contributors) { this.contributors = contributors; }
        public List<FileChangeStat> getFrequentlyChangedFiles() { return frequentlyChangedFiles; }
        public void setFrequentlyChangedFiles(List<FileChangeStat> frequentlyChangedFiles) { this.frequentlyChangedFiles = frequentlyChangedFiles; }
        public List<CoChangeStat> getFilesChangedTogether() { return filesChangedTogether; }
        public void setFilesChangedTogether(List<CoChangeStat> filesChangedTogether) { this.filesChangedTogether = filesChangedTogether; }
        public String getError() { return error; }
        public void setError(String error) { this.error = error; }
    }
}
