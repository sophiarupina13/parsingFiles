package org.example;

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
        System.out.print("Enter the root directory path: \n");
        rootDirectory = scanner.nextLine().replace("/", "\\");
        scanner.close();

        try {
            Path rootPath = Paths.get(rootDirectory);
            revealFileDependencies(rootPath);
            concatenateFiles();
            System.out.println("The result is in 'output.txt' file");
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

                    if (!fileIsInStack) {

                    filesDependencies.putIfAbsent(relativePath, new ArrayList<>());

                        try {
                            List<String> lines = Files.readAllLines(filePath, StandardCharsets.UTF_8);
                            for (String line : lines) {
                                if (line.startsWith("require")) {
                                    String requiredFilePath = line.split("‘|’")[1];
                                    filesDependencies.get(relativePath).add(requiredFilePath);
                                }
                            }
                            var fileDependencies = filesDependencies.get(relativePath);
                            updateStructure(relativePath, fileDependencies);
                            if (!sortedFiles.contains(relativePath)) sortedFiles.add(relativePath);
                        } catch (IOException e) {
                            System.out.println("Cannot read files");
                        }
                    }

                });
    }

    private static void updateStructure(String filePath, List<String> fileDependencies) {
        for (String dependency : fileDependencies) {
            dependency = dependency.replace("/", "\\") + ".txt";
            if (!sortedFiles.contains(dependency)) {
                sortedFiles.add(dependency);
            }
        }
        if (!sortedFiles.contains(filePath)) sortedFiles.add(filePath);
        else System.out.println("Cycle dependency detected");
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
        } catch (IOException e) {
            System.out.println("Cannot write final file");
        }

    }
}