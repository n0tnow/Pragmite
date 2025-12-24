package com.pragmite.refactor.strategies;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.pragmite.model.CodeSmell;
import com.pragmite.model.CodeSmellType;
import com.pragmite.refactor.RefactoringStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Pattern;

/**
 * Suggests renaming variables that have poor or inconsistent names.
 * Identifies variables with single-character names, Hungarian notation, or other naming issues.
 */
public class RenameVariableStrategy implements RefactoringStrategy {

    private static final Logger logger = LoggerFactory.getLogger(RenameVariableStrategy.class);
    private static final Pattern HUNGARIAN = Pattern.compile("^(str|int|bool|obj|arr|lst|dbl|flt)[A-Z]");
    private static final Pattern SINGLE_CHAR = Pattern.compile("^[a-z]$");
    private static final Pattern NON_DESCRIPTIVE = Pattern.compile("^(temp|tmp|val|var|data|info)\\d*$");

    @Override
    public String getName() {
        return "Rename Variable";
    }

    @Override
    public boolean canHandle(CodeSmell smell) {
        return smell.getType() == CodeSmellType.UNUSED_VARIABLE;
    }

    @Override
    public boolean validate(CodeSmell smell) throws Exception {
        Path filePath = Paths.get(smell.getFilePath());

        if (!Files.exists(filePath)) {
            logger.warn("File does not exist: {}", filePath);
            return false;
        }

        return true;
    }

    @Override
    public String apply(CodeSmell smell) throws Exception {
        Path filePath = Paths.get(smell.getFilePath());
        String sourceCode = Files.readString(filePath);
        CompilationUnit cu = com.github.javaparser.StaticJavaParser.parse(sourceCode);

        StringBuilder report = new StringBuilder();
        int poorlyNamedVars = 0;

        for (VariableDeclarator var : cu.findAll(VariableDeclarator.class)) {
            String name = var.getNameAsString();
            String suggestion = analyzeName(name);

            if (suggestion != null) {
                poorlyNamedVars++;
                report.append(String.format("Variable '%s' at line %d: %s%n",
                                          name,
                                          var.getBegin().get().line,
                                          suggestion));
            }
        }

        if (poorlyNamedVars > 0) {
            logger.info("Found {} poorly named variable(s) in {}", poorlyNamedVars, filePath.getFileName());
            return String.format("Found %d variable(s) with poor naming:%n%s",
                               poorlyNamedVars, report.toString());
        } else {
            return "No variables found with naming issues";
        }
    }

    private String analyzeName(String name) {
        // Skip common loop variables
        if (name.equals("i") || name.equals("j") || name.equals("k")) {
            return null;
        }

        if (HUNGARIAN.matcher(name).find()) {
            return "Uses Hungarian notation - consider using descriptive name without type prefix";
        }

        if (SINGLE_CHAR.matcher(name).matches()) {
            return "Single-character variable name - use descriptive name";
        }

        if (NON_DESCRIPTIVE.matcher(name).matches()) {
            return "Non-descriptive name - use meaningful business domain name";
        }

        if (name.length() > 30) {
            return "Variable name is too long - consider shortening while keeping it descriptive";
        }

        return null;
    }

    @Override
    public String getDescription() {
        return "Rename Variable: Identifies variables with poor or inconsistent names";
    }
}
