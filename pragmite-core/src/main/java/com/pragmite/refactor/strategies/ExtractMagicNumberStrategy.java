package com.pragmite.refactor.strategies;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.pragmite.model.CodeSmell;
import com.pragmite.model.CodeSmellType;
import com.pragmite.refactor.RefactoringStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extracts magic numbers into named constants.
 * Converts hardcoded numeric literals into descriptive static final constants.
 */
public class ExtractMagicNumberStrategy implements RefactoringStrategy {
    private static final Logger logger = LoggerFactory.getLogger(ExtractMagicNumberStrategy.class);

    private static final Pattern NUMBER_PATTERN = Pattern.compile(
        "\\b(0x[0-9A-Fa-f]+|0b[01]+|\\d+\\.\\d+[fFdD]?|\\d+[LlFfDd]?)\\b");

    @Override
    public String getName() {
        return "Extract Magic Number";
    }

    @Override
    public boolean canHandle(CodeSmell smell) {
        return smell.getType() == CodeSmellType.MAGIC_NUMBER;
    }

    @Override
    public boolean validate(CodeSmell smell) throws Exception {
        Path filePath = Paths.get(smell.getFilePath());

        if (!Files.exists(filePath)) {
            return false;
        }

        if (!Files.isWritable(filePath)) {
            return false;
        }

        // Check if we can parse the file
        String content = Files.readString(filePath);
        JavaParser parser = new JavaParser();
        ParseResult<CompilationUnit> result = parser.parse(content);

        return result.isSuccessful();
    }

    @Override
    public String apply(CodeSmell smell) throws Exception {
        Path filePath = Paths.get(smell.getFilePath());
        String content = Files.readString(filePath);
        String[] lines = content.split("\n");

        int lineIndex = smell.getStartLine() - 1;

        if (lineIndex < 0 || lineIndex >= lines.length) {
            throw new IllegalArgumentException("Invalid line number: " + smell.getStartLine());
        }

        String line = lines[lineIndex];

        // Extract the magic number from the description
        String magicNumber = extractMagicNumber(smell.getDescription());

        if (magicNumber == null) {
            throw new IllegalStateException("Could not extract magic number from: " + smell.getDescription());
        }

        // Generate constant name
        String constantName = generateConstantName(magicNumber, smell.getAffectedElement());

        // Check if constant already exists
        if (constantAlreadyExists(content, constantName)) {
            // Just replace usage
            String replacedLine = line.replaceFirst(
                Pattern.quote(magicNumber),
                constantName
            );
            lines[lineIndex] = replacedLine;
        } else {
            // Add constant declaration and replace usage
            String constantDeclaration = generateConstantDeclaration(constantName, magicNumber);

            // Find where to insert the constant (after class declaration)
            int insertIndex = findConstantInsertionPoint(lines);

            // Insert constant
            String[] newLines = new String[lines.length + 1];
            System.arraycopy(lines, 0, newLines, 0, insertIndex);
            newLines[insertIndex] = constantDeclaration;
            System.arraycopy(lines, insertIndex, newLines, insertIndex + 1, lines.length - insertIndex);

            // Update line index after insertion
            if (lineIndex >= insertIndex) {
                lineIndex++;
            }

            // Replace usage
            newLines[lineIndex] = newLines[lineIndex].replaceFirst(
                Pattern.quote(magicNumber),
                constantName
            );

            lines = newLines;
        }

        // Write back
        Files.writeString(filePath, String.join("\n", lines));

        logger.info("Extracted magic number {} to constant {} in {}",
            magicNumber, constantName, filePath.getFileName());

        return String.format("Extracted magic number %s to constant %s", magicNumber, constantName);
    }

    @Override
    public String getDescription() {
        return "Extracts magic numbers into named static final constants";
    }

    /**
     * Extracts the numeric value from the code smell description.
     */
    private String extractMagicNumber(String description) {
        // Description format: "Magic number: 3.14159" or similar
        Matcher matcher = NUMBER_PATTERN.matcher(description);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    /**
     * Generates a descriptive constant name based on the number and context.
     */
    private String generateConstantName(String number, String context) {
        // Try to infer meaning from context
        if (number.equals("3.14159") || number.equals("3.14") || number.startsWith("3.1415")) {
            return "PI";
        }
        if (number.equals("2.71828") || number.equals("2.718")) {
            return "E";
        }
        if (number.equals("0.5") || number.equals("0.5f")) {
            return "HALF";
        }
        if (number.equals("100") || number.equals("100.0")) {
            return "PERCENT_MAX";
        }
        if (number.equals("1000") || number.equals("1000L")) {
            return "THOUSAND";
        }

        // Generic name based on value
        String sanitized = number.replaceAll("[^0-9A-Za-z]", "_");
        return "CONSTANT_" + sanitized.toUpperCase();
    }

    /**
     * Generates the constant declaration line.
     */
    private String generateConstantDeclaration(String name, String value) {
        String type = inferType(value);
        return String.format("    private static final %s %s = %s;", type, name, value);
    }

    /**
     * Infers the Java type from the numeric literal.
     */
    private String inferType(String value) {
        if (value.endsWith("L") || value.endsWith("l")) {
            return "long";
        }
        if (value.endsWith("F") || value.endsWith("f")) {
            return "float";
        }
        if (value.endsWith("D") || value.endsWith("d") || value.contains(".")) {
            return "double";
        }
        if (value.startsWith("0x") || value.startsWith("0X")) {
            return "int";
        }
        if (value.startsWith("0b") || value.startsWith("0B")) {
            return "int";
        }
        return "int";
    }

    /**
     * Finds the best location to insert the constant declaration.
     */
    private int findConstantInsertionPoint(String[] lines) {
        // Find first field or method declaration after class opening brace
        boolean inClass = false;
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();

            if (line.contains("class ") && line.contains("{")) {
                inClass = true;
                return i + 1; // Right after class declaration
            }

            if (inClass && !line.isEmpty() && !line.startsWith("//") && !line.startsWith("/*")) {
                return i; // Before first non-comment line in class
            }
        }

        return 1; // Fallback to line 1
    }

    /**
     * Checks if a constant with the given name already exists in the file.
     */
    private boolean constantAlreadyExists(String content, String constantName) {
        Pattern pattern = Pattern.compile(
            "private\\s+static\\s+final\\s+\\w+\\s+" + Pattern.quote(constantName) + "\\s*=");
        return pattern.matcher(content).find();
    }
}
