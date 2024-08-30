package org.example;

import org.example.CycleException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class ParsingFiles {

    private static String rootDirectory = "";

    private static final Map<String, List<String>> filesDependencies = new HashMap<>();
    private static final List<String> sortedFiles = new ArrayList<>();

    public static void main(String[] args) throws IOException {

        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter the root directory path:\n");
        rootDirectory = scanner.nextLine().replace("/", "\\");
        scanner.close();

        try {
            Path rootPath = Paths.get(rootDirectory);
            revealFileDependencies(rootPath);
            concatenateFiles();
        } catch (IOException e) {
            System.out.println("Error processing files: " + e.getMessage());
        }

    }

    private static void revealFileDependencies(Path rootPath) throws IOException {
        Files.walk(rootPath)
                .filter(Files::isRegularFile)
                .forEach(filePath -> {

                    String relativePath = rootPath.relativize(filePath).toString();

                    boolean fileIsInStack = sortedFiles.contains(relativePath);

                    filesDependencies.putIfAbsent(relativePath, new ArrayList<>());

                    try {
                        List<String> lines = Files.readAllLines(filePath, StandardCharsets.UTF_8);
                        for (String line : lines) {
                            if (line.startsWith("require")) {
                                String requiredFilePath = line.split("‘|’")[1].replace("/", "\\") + ".txt";
                                filesDependencies.get(relativePath).add(requiredFilePath);
                            }
                        }
                        var fileDependencies = filesDependencies.get(relativePath);
                        if (!fileIsInStack) {
                            updateStructure(relativePath, fileDependencies);
                        }
                        else {
                            if (checkForCycleDependency(relativePath, fileDependencies)) {
                                System.out.println("Cycle detected: " + sortedFiles);
                                throw new CycleException("Cycle detected: " + sortedFiles);
                            }
                        }

                    } catch (IOException e) {
                        System.out.println("Cannot read files");
                    } catch (CycleException e) {
                        throw new RuntimeException(e);
                    }
                });
    }

    private static void updateStructure(String filePath, List<String> fileDependencies) {
        for (String dependency : fileDependencies) {
            if (!sortedFiles.contains(dependency)) {
                sortedFiles.add(dependency);
            }
        }
        if (!sortedFiles.contains(filePath)) sortedFiles.add(filePath);
    }

    private static boolean checkForCycleDependency(String relativePath, List<String> fileDependencies) {
        for (String key : filesDependencies.keySet()) {
            if (fileDependencies.contains(key) && filesDependencies.get(key).contains(relativePath)) {
                sortedFiles.add(relativePath);
                return true;
            }
        }
        return false;
    }

    private static void concatenateFiles() {
        StringBuilder result = new StringBuilder();
        for (String filePath : sortedFiles) {
            Path fullPath = Paths.get(rootDirectory, filePath);
            try {
                List<String> lines = Files.readAllLines(fullPath, StandardCharsets.UTF_8);
                for (String line : lines) {
                    if (!line.startsWith("require")) {
                        result.append(line).append(System.lineSeparator());
                    }
                }
            } catch (IOException e) {
                System.out.println("Cannot read files");
            }
        }

        try {
            Files.write(Paths.get(rootDirectory, "output.txt"), result.toString().getBytes(StandardCharsets.UTF_8));
            System.out.println("The result is in 'output.txt' file");
        } catch (IOException e) {
            System.out.println("Cannot write final file");
        }

    }
}