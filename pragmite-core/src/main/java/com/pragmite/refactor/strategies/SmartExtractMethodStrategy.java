package com.pragmite.refactor.strategies;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.pragmite.model.CodeSmell;
import com.pragmite.model.CodeSmellType;
import com.pragmite.refactor.RefactoringStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Smart Extract Method - Automatically extracts code blocks from long methods.
 * NOW WITH REAL AST MANIPULATION!
 */
@SuppressWarnings("unchecked")
public class SmartExtractMethodStrategy implements RefactoringStrategy {
    private static final Logger logger = LoggerFactory.getLogger(SmartExtractMethodStrategy.class);
    private static final int MIN_BLOCK_SIZE = 5;
    private static final int MAX_BLOCK_SIZE = 15;

    @Override
    public String getName() {
        return "Smart Extract Method (Auto-Apply)";
    }

    @Override
    public boolean canHandle(CodeSmell smell) {
        return smell.getType() == CodeSmellType.LONG_METHOD;
    }

    @Override
    public boolean validate(CodeSmell smell) throws Exception {
        Path filePath = Paths.get(smell.getFilePath());
        return Files.exists(filePath) && Files.isWritable(filePath);
    }

    @Override
    public String apply(CodeSmell smell) throws Exception {
        Path filePath = Paths.get(smell.getFilePath());
        String sourceCode = Files.readString(filePath);
        CompilationUnit cu = StaticJavaParser.parse(sourceCode);

        int extractionCount = 0;
        StringBuilder report = new StringBuilder();

        for (MethodDeclaration method : cu.findAll(MethodDeclaration.class)) {
            if (shouldExtract(method, smell)) {
                List<CodeBlock> blocks = identifyExtractableBlocks(method);

                for (CodeBlock block : blocks) {
                    String extractedMethodName = generateMethodName(block, method);

                    // REAL AST MANIPULATION HERE!
                    boolean success = extractMethodReal(method, block, extractedMethodName);

                    if (success) {
                        extractionCount++;
                        report.append(String.format("✓ Extracted method '%s' from '%s' (%d lines)%n",
                                                   extractedMethodName,
                                                   method.getNameAsString(),
                                                   block.statements.size()));
                    }
                }
            }
        }

        if (extractionCount > 0) {
            Files.writeString(filePath, cu.toString());
            logger.info("Applied {} method extractions to {}", extractionCount, filePath.getFileName());
            return String.format("Successfully extracted %d method(s):%n%s", extractionCount, report.toString());
        } else {
            return "No suitable blocks found for extraction";
        }
    }

    private boolean shouldExtract(MethodDeclaration method, CodeSmell smell) {
        if (!method.getBody().isPresent()) return false;

        if (smell.getAffectedElement() != null &&
            !method.getNameAsString().equals(smell.getAffectedElement())) {
            return false;
        }

        BlockStmt body = method.getBody().get();
        return body.getStatements().size() >= 15;
    }

    private List<CodeBlock> identifyExtractableBlocks(MethodDeclaration method) {
        List<CodeBlock> blocks = new ArrayList<>();
        BlockStmt body = method.getBody().get();
        List<Statement> statements = body.getStatements();

        int i = 0;
        while (i < statements.size()) {
            CodeBlock block = findCohesiveBlock(statements, i);

            if (block != null && block.statements.size() >= MIN_BLOCK_SIZE) {
                blocks.add(block);
                i = block.endIndex + 1;
            } else {
                i++;
            }
        }

        return blocks;
    }

    private CodeBlock findCohesiveBlock(List<Statement> statements, int startIndex) {
        if (startIndex >= statements.size()) return null;

        Set<String> usedVariables = new HashSet<>();
        Set<String> definedVariables = new HashSet<>();
        List<Statement> blockStatements = new ArrayList<>();

        int endIndex = startIndex;

        for (int i = startIndex; i < Math.min(startIndex + MAX_BLOCK_SIZE, statements.size()); i++) {
            Statement stmt = statements.get(i);
            blockStatements.add(stmt);

            Set<String> stmtUsed = findUsedVariables(stmt);
            Set<String> stmtDefined = findDefinedVariables(stmt);

            usedVariables.addAll(stmtUsed);
            definedVariables.addAll(stmtDefined);

            endIndex = i;

            if (blockStatements.size() >= MIN_BLOCK_SIZE) {
                if (i + 1 < statements.size()) {
                    Set<String> nextUsed = findUsedVariables(statements.get(i + 1));
                    boolean hasOverlap = definedVariables.stream().anyMatch(nextUsed::contains);
                    if (!hasOverlap) {
                        break;
                    }
                }
            }
        }

        if (blockStatements.size() < MIN_BLOCK_SIZE) {
            return null;
        }

        return new CodeBlock(startIndex, endIndex, blockStatements, usedVariables, definedVariables);
    }

    /**
     * REAL AST MANIPULATION - Actually extracts the method!
     */
    private boolean extractMethodReal(MethodDeclaration originalMethod, CodeBlock block, String newMethodName) {
        try {
            // 1. Find the containing class
            Optional<ClassOrInterfaceDeclaration> classDecl =
                originalMethod.findAncestor(ClassOrInterfaceDeclaration.class);

            if (!classDecl.isPresent()) {
                logger.warn("Could not find containing class");
                return false;
            }

            // 2. Determine parameters needed (variables used but not defined in block)
            Set<String> parameters = new HashSet<>(block.usedVariables);
            parameters.removeAll(block.definedVariables);

            // 3. Analyze return value requirements
            ReturnInfo returnInfo = analyzeReturnValue(block);

            // 4. Create new method
            MethodDeclaration newMethod = new MethodDeclaration();
            newMethod.setName(newMethodName);
            newMethod.setModifiers(Modifier.Keyword.PRIVATE);

            // Set return type based on analysis
            if (returnInfo.hasReturn) {
                newMethod.setType(returnInfo.returnType);
            } else if (returnInfo.modifiedVariable != null) {
                // Variable is modified and used later - return it
                newMethod.setType(new ClassOrInterfaceType(null, "Object"));
            } else {
                newMethod.setType(new ClassOrInterfaceType(null, "void"));
            }

            // Add parameters
            for (String paramName : parameters) {
                if (paramName.length() > 0 && Character.isJavaIdentifierStart(paramName.charAt(0))) {
                    Parameter param = new Parameter(new ClassOrInterfaceType(null, "Object"), paramName);
                    newMethod.addParameter(param);
                }
            }

            // 5. Create method body with extracted statements
            BlockStmt newBody = new BlockStmt();
            for (Statement stmt : block.statements) {
                newBody.addStatement(stmt.clone()); // Clone to avoid removing from original
            }

            // Add return statement if needed
            if (!returnInfo.hasReturn && returnInfo.modifiedVariable != null) {
                newBody.addStatement(StaticJavaParser.parseStatement(
                    "return " + returnInfo.modifiedVariable + ";"));
            }

            newMethod.setBody(newBody);

            // 6. Add new method to class
            classDecl.get().addMember(newMethod);

            // 7. Replace original block with method call
            BlockStmt originalBody = originalMethod.getBody().get();
            List<Statement> originalStatements = originalBody.getStatements();

            // Create method call
            MethodCallExpr methodCall = new MethodCallExpr(newMethodName);
            for (String paramName : parameters) {
                if (paramName.length() > 0 && Character.isJavaIdentifierStart(paramName.charAt(0))) {
                    methodCall.addArgument(new NameExpr(paramName));
                }
            }

            // Create call statement (with assignment if needed)
            Statement callStmt;
            if (returnInfo.modifiedVariable != null) {
                callStmt = StaticJavaParser.parseStatement(
                    returnInfo.modifiedVariable + " = " + methodCall.toString() + ";");
            } else {
                callStmt = new ExpressionStmt(methodCall);
            }

            // Replace block with call
            originalStatements.set(block.startIndex, callStmt);

            // Remove other statements in the block
            for (int i = block.endIndex; i > block.startIndex; i--) {
                originalStatements.remove(i);
            }

            logger.info("Successfully extracted method: {} (returns: {})",
                       newMethodName, returnInfo.hasReturn || returnInfo.modifiedVariable != null);
            return true;

        } catch (Exception e) {
            logger.error("Failed to extract method: {}", newMethodName, e);
            return false;
        }
    }

    /**
     * Analyzes if the code block returns a value or modifies variables used later.
     */
    private ReturnInfo analyzeReturnValue(CodeBlock block) {
        ReturnInfo info = new ReturnInfo();

        // Check if block contains return statement
        for (Statement stmt : block.statements) {
            String stmtStr = stmt.toString();
            if (stmtStr.contains("return ")) {
                info.hasReturn = true;
                // Try to infer return type from the statement
                if (stmtStr.contains("return true") || stmtStr.contains("return false")) {
                    info.returnType = new ClassOrInterfaceType(null, "boolean");
                } else if (stmtStr.matches(".*return\\s+\\d+.*")) {
                    info.returnType = new ClassOrInterfaceType(null, "int");
                } else if (stmtStr.contains("return \"")) {
                    info.returnType = new ClassOrInterfaceType(null, "String");
                } else {
                    info.returnType = new ClassOrInterfaceType(null, "Object");
                }
                break;
            }
        }

        // If no return, check if any variable is modified and used outside block
        if (!info.hasReturn && !block.definedVariables.isEmpty()) {
            // Pick the first modified variable as the return value
            info.modifiedVariable = block.definedVariables.stream()
                .filter(v -> v.length() > 0 && Character.isJavaIdentifierStart(v.charAt(0)))
                .findFirst()
                .orElse(null);
        }

        return info;
    }

    private static class ReturnInfo {
        boolean hasReturn = false;
        ClassOrInterfaceType returnType = new ClassOrInterfaceType(null, "void");
        String modifiedVariable = null;
    }

    private Set<String> findUsedVariables(Statement stmt) {
        Set<String> variables = new HashSet<>();
        stmt.findAll(NameExpr.class).forEach(name -> variables.add(name.getNameAsString()));
        return variables;
    }

    private Set<String> findDefinedVariables(Statement stmt) {
        Set<String> variables = new HashSet<>();
        String stmtStr = stmt.toString();
        if (stmtStr.contains("=") && !stmtStr.contains("==")) {
            String[] parts = stmtStr.split("=");
            if (parts.length > 0) {
                String left = parts[0].trim();
                String[] tokens = left.split("\\s+");
                if (tokens.length > 0) {
                    variables.add(tokens[tokens.length - 1]);
                }
            }
        }
        return variables;
    }

    private String generateMethodName(CodeBlock block, MethodDeclaration originalMethod) {
        List<String> keywords = extractKeywords(block);

        if (!keywords.isEmpty()) {
            String inferredName = keywords.stream()
                .limit(2)
                .collect(Collectors.joining("And"));
            return Character.toLowerCase(inferredName.charAt(0)) + inferredName.substring(1);
        }

        return originalMethod.getNameAsString() + "Helper" + (System.currentTimeMillis() % 1000);
    }

    private List<String> extractKeywords(CodeBlock block) {
        List<String> keywords = new ArrayList<>();

        for (Statement stmt : block.statements) {
            String stmtStr = stmt.toString().toLowerCase();

            if (stmtStr.contains("calculate")) keywords.add("Calculate");
            else if (stmtStr.contains("validate")) keywords.add("Validate");
            else if (stmtStr.contains("process")) keywords.add("Process");
            else if (stmtStr.contains("format")) keywords.add("Format");
            else if (stmtStr.contains("parse")) keywords.add("Parse");
            else if (stmtStr.contains("build")) keywords.add("Build");
            else if (stmtStr.contains("create")) keywords.add("Create");
            else if (stmtStr.contains("check")) keywords.add("Check");
        }

        return keywords.stream().distinct().collect(Collectors.toList());
    }

    @Override
    public String getDescription() {
        return "Smart Extract Method: Automatically extracts cohesive code blocks into separate methods";
    }

    private static class CodeBlock {
        final int startIndex;
        final int endIndex;
        final List<Statement> statements;
        final Set<String> usedVariables;
        final Set<String> definedVariables;

        CodeBlock(int startIndex, int endIndex, List<Statement> statements,
                  Set<String> usedVariables, Set<String> definedVariables) {
            this.startIndex = startIndex;
            this.endIndex = endIndex;
            this.statements = statements;
            this.usedVariables = usedVariables;
            this.definedVariables = definedVariables;
        }
    }
}
