package com.pragmite.refactor.strategies;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.pragmite.model.CodeSmell;
import com.pragmite.model.CodeSmellType;
import com.pragmite.refactor.RefactoringStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Suggests parameter objects for methods with long parameter lists.
 * Groups related parameters into a cohesive object.
 */
public class IntroduceParameterObjectStrategy implements RefactoringStrategy {

    private static final Logger logger = LoggerFactory.getLogger(IntroduceParameterObjectStrategy.class);
    private static final int MIN_PARAMETERS_FOR_OBJECT = 4;

    @Override
    public String getName() {
        return "Introduce Parameter Object";
    }

    @Override
    public boolean canHandle(CodeSmell smell) {
        return smell.getType() == CodeSmellType.LONG_PARAMETER_LIST ||
               smell.getType() == CodeSmellType.PRIMITIVE_OBSESSION ||
               smell.getType() == CodeSmellType.DATA_CLUMPS;
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
        int candidateMethods = 0;

        for (MethodDeclaration method : cu.findAll(MethodDeclaration.class)) {
            if (shouldIntroduceParameterObject(method)) {
                candidateMethods++;
                String suggestion = analyzeParameterObject(method);
                report.append(suggestion).append("\n");
            }
        }

        if (candidateMethods > 0) {
            logger.info("Found {} method(s) that could use parameter objects in {}",
                       candidateMethods, filePath.getFileName());
            return String.format("Found %d method(s) with long parameter lists that could use parameter objects:%n%s",
                               candidateMethods, report.toString());
        } else {
            return "No methods found with long parameter lists";
        }
    }

    private boolean shouldIntroduceParameterObject(MethodDeclaration method) {
        return method.getParameters().size() >= MIN_PARAMETERS_FOR_OBJECT;
    }

    private String analyzeParameterObject(MethodDeclaration method) {
        List<Parameter> params = method.getParameters();

        List<String> primitiveParams = params.stream()
            .filter(p -> isPrimitive(p.getTypeAsString()))
            .map(Parameter::getNameAsString)
            .collect(Collectors.toList());

        String className = generateParameterObjectName(method);
        String paramObjectCode = generateParameterObjectClass(className, params);

        return String.format("Method '%s' (line %d): %d parameters%n" +
                           "  Suggested parameter object class: %s%n" +
                           "  Primitive parameters: %s%n" +
                           "  Class template:%n%s",
                           method.getNameAsString(),
                           method.getBegin().get().line,
                           params.size(),
                           className,
                           String.join(", ", primitiveParams),
                           paramObjectCode);
    }

    private String generateParameterObjectName(MethodDeclaration method) {
        String methodName = method.getNameAsString();
        String baseName = methodName.substring(0, 1).toUpperCase() + methodName.substring(1);
        return baseName + "Parameters";
    }

    private String generateParameterObjectClass(String className, List<Parameter> params) {
        StringBuilder code = new StringBuilder();
        code.append("    public class ").append(className).append(" {\n");

        for (Parameter param : params) {
            if (isPrimitive(param.getTypeAsString())) {
                code.append("      private final ").append(param.getTypeAsString())
                    .append(" ").append(param.getNameAsString()).append(";\n");
            }
        }

        code.append("      // Constructor, getters...\n");
        code.append("    }");

        return code.toString();
    }

    private boolean isPrimitive(String type) {
        return type.equals("int") || type.equals("long") || type.equals("double") ||
               type.equals("float") || type.equals("boolean") || type.equals("char") ||
               type.equals("byte") || type.equals("short") || type.equals("String");
    }

    @Override
    public String getDescription() {
        return "Introduce Parameter Object: Suggests grouping related parameters into cohesive objects";
    }
}
