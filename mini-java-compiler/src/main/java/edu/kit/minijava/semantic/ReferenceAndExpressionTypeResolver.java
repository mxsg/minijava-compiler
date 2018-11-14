package edu.kit.minijava.semantic;

import java.util.*;

import edu.kit.minijava.ast.nodes.*;
import edu.kit.minijava.ast.references.TypeOfExpression;

/**
 * Because MiniJava allows use-before-declare, we need two passes: one for collecting class, method and field
 * declarations and one to resolve all references and expression types.
 *
 * Before visiting an expression, its type is not resolved. After visiting an expression, its type must be resolved.
 */
public class ReferenceAndExpressionTypeResolver extends SemanticAnalysisVisitorBase {
    public ReferenceAndExpressionTypeResolver(Program program) {
        program.accept(this, null);

        this.finishCollectingClassDeclarations();

        program.accept(this, null);

        this.finishCollectingClassMemberDeclarations();

        program.accept(this, null);

        assert this.getEntryPoint().isPresent() : "missing main method";
    }

    // MARK: - Traversal

    @Override
    protected void visit(Program program, Void context) {
        for (ClassDeclaration clsDecl : program.getClassDeclarations()) {
            clsDecl.accept(this, context);
        }
    }

    @Override
    protected void visit(ClassDeclaration classDeclaration, Void context) {
        if (this.isCollectingClassDeclarations()) {
            this.registerClassDeclaration(classDeclaration);
        }
        else {
            this.enterClassDeclaration(classDeclaration);

            classDeclaration.getFieldDeclarations().forEach(node -> node.accept(this, context));
            classDeclaration.getMethodDeclarations().forEach(node -> node.accept(this, context));
            classDeclaration.getMainMethodDeclarations().forEach(node -> node.accept(this, context));

            this.leaveCurrentClassDeclaration();
        }
    }

    @Override
    protected void visit(FieldDeclaration fieldDeclaration, Void context) {
        if (this.isCollectingClassMemberDeclarations()) {
            fieldDeclaration.getType().accept(this, context);

            // Field types must not be void or array of void.
            assert !(fieldDeclaration.getType().isVoid() || fieldDeclaration.getType().isDimensionalVoid()) :
                    "field must not be void or array of void";

            this.registerFieldDeclaration(fieldDeclaration, this.getCurrentClassDeclaration());
        }
        else {
            this.addVariableDeclarationToCurrentScope(fieldDeclaration);
        }
    }

    @Override
    protected void visit(MethodDeclaration methodDeclaration, Void context) {
        if (this.isCollectingClassMemberDeclarations()) {
            this.enterMethodDeclaration(methodDeclaration);

            methodDeclaration.getReturnType().accept(this, context);

            // Return type must not be array of void.
            assert !methodDeclaration.getReturnType().isDimensionalVoid() : "must not be array of void";

            methodDeclaration.getParameters().forEach(node -> node.accept(this, context));

            this.leaveCurrentMethodDeclaration();
            this.registerMethodDeclaration(methodDeclaration, this.getCurrentClassDeclaration());
        }
        else {
            this.enterMethodDeclaration(methodDeclaration);

            // Parameter types are already resolved, but we need to add the variable declarations to current scope.
            methodDeclaration.getParameters().forEach(node -> node.accept(this, context));

            methodDeclaration.getBody().accept(this, context);

            // Non-void methods must return a value.
            if (!methodDeclaration.getReturnType().isVoid()) {
                assert methodDeclaration.getBody().explicitlyReturns() : "must return a value";
            }

            // Methods must not contain unreachable statements.
            assert !methodDeclaration.getBody().containsUnreachableStatements() : "contains unreachable statements";

            this.leaveCurrentMethodDeclaration();
        }
    }

    @Override
    protected void visit(MainMethodDeclaration methodDeclaration, Void context) {
        if (this.isCollectingClassMemberDeclarations()) {
            this.enterMethodDeclaration(methodDeclaration);

            // Main method must be named main.
            assert methodDeclaration.getName().equals("main") : "invalid main method name";

            methodDeclaration.getReturnType().accept(this, context);

            // Main method must return void, but AST doesn't guarantee it.
            assert methodDeclaration.getReturnType().isVoid() : "must be void";

            methodDeclaration.getArgumentsParameter().accept(this, context);

            // Main method must take array of strings, but AST doesn't guarantee it.
            assert methodDeclaration.getArgumentsParameter().getType().isArrayOfString() : "must be array of string";

            this.leaveCurrentMethodDeclaration();
            this.setEntryPoint(methodDeclaration);
        }
        else {
            this.enterMethodDeclaration(methodDeclaration);

            // Parameter types are already resolved, but we need to add the variable declarations to current scope.
            methodDeclaration.getArgumentsParameter().accept(this, context);

            methodDeclaration.getBody().accept(this, context);

            this.leaveCurrentMethodDeclaration();
        }
    }

    @Override
    protected void visit(ParameterDeclaration parameterDeclaration, Void context) {
        if (this.isCollectingClassMemberDeclarations()) {
            parameterDeclaration.getType().accept(this, context);

            // Parameter types must not be void or array of void.
            assert !(parameterDeclaration.getType().isVoid() || parameterDeclaration.getType().isDimensionalVoid()) :
                    "parameter must not be void or array of void";
        }
        else {
            this.addVariableDeclarationToCurrentScope(parameterDeclaration);
        }
    }

    @Override
    protected void visit(ExplicitTypeReference reference, Void context) {
        String name = reference.getBasicTypeReference().getName();
        Optional<BasicTypeDeclaration> declaration = this.getBasicTypeDeclarationForName(name);

        // Type reference must be resolvable.
        assert declaration.isPresent() : "Use of undeclared type: `" + name + "`";

        reference.getBasicTypeReference().resolveTo(declaration.get());
    }

    @Override
    protected void visit(ImplicitTypeReference reference, Void context) {
        // Implicit references are already resolved upon creation.
    }

    @Override
    protected void visit(Statement.IfStatement statement, Void context) {
        statement.getCondition().accept(this, context);

        // Condition for if statement must be boolean.
        assert statement.getCondition().getType().isBoolean() : "error";

        // AST doesn't guarantee child statements not being local variable declaration, only ASTs vended from Parser do.
        this.enterNewVariableDeclarationScope();
        statement.getStatementIfTrue().accept(this, context);
        this.leaveCurrentVariableDeclarationScope();
        this.enterNewVariableDeclarationScope();
        statement.getStatementIfFalse().ifPresent(node -> node.accept(this, context));
        this.leaveCurrentVariableDeclarationScope();
    }

    @Override
    protected void visit(Statement.WhileStatement statement, Void context) {
        statement.getCondition().accept(this, context);

        // Condition for while statement must be boolean.
        assert statement.getCondition().getType().isBoolean() : "error";

        // AST doesn't guarantee child statements not being local variable declaration, only ASTs vended from Parser do.
        this.enterNewVariableDeclarationScope();
        statement.getStatementWhileTrue().accept(this, context);
        this.leaveCurrentVariableDeclarationScope();
    }

    @Override
    protected void visit(Statement.ExpressionStatement statement, Void context) {
        statement.getExpression().accept(this, context);

        // Expression must be valid for expression statement.
        assert statement.getExpression().isValidForExpressionStatement() : "not a statement";
    }

    @Override
    protected void visit(Statement.ReturnStatement statement, Void context) {
        statement.getValue().ifPresent(node -> node.accept(this, context));

        TypeReference expectedReturnType = this.getCurrentMethodDeclaration().getReturnType();

        if (statement.getValue().isPresent()) {
            TypeOfExpression actualReturnType = statement.getValue().get().getType();

            // Return value must be compatible with expected return type.
            assert canAssignTypeOfExpressionToTypeReference(actualReturnType, expectedReturnType) : "invalid retval";
        }
        else {
            // Plain return is only allowed in void methods.
            assert expectedReturnType.isVoid() : "must return value from non-void function";
        }
    }

    @Override
    protected void visit(Statement.EmptyStatement statement, Void context) {}

    @Override
    protected void visit(Statement.LocalVariableDeclarationStatement statement, Void context) {
        statement.getType().accept(this, context);

        // Variables must not be of type void or array of void.
        assert !(statement.getType().isVoid() || statement.getType().isDimensionalVoid()) :
                "variable must not be void or array of void";

        // TODO: Is the variable declaration known when evaluating the initial value expression?
        this.addVariableDeclarationToCurrentScope(statement);

        if (statement.getValue().isPresent()) {
            statement.getValue().get().accept(this, context);

            // Type of initial value must be compatible with variable declaration.
            assert canAssignTypeOfExpressionToTypeReference(statement.getValue().get().getType(), statement.getType()) :
                    "incompatible types for assignment";
        }
    }

    @Override
    protected void visit(Statement.Block block, Void context) {
        this.enterNewVariableDeclarationScope();

        block.getStatements().forEach(node -> node.accept(this, context));

        this.leaveCurrentVariableDeclarationScope();
    }

    @Override
    protected void visit(Expression.BinaryOperation expression, Void context) {
        expression.getLeft().accept(this, context);
        expression.getRight().accept(this, context);

        TypeOfExpression typeOfLeftOperand = expression.getLeft().getType();
        TypeOfExpression typeOfRightOperand = expression.getRight().getType();

        switch (expression.getOperationType()) {
            case MULTIPLICATION:
            case DIVISION:
            case MODULO:
            case ADDITION:
            case SUBTRACTION:
                // Operands for numeric operations must be integers.
                assert typeOfLeftOperand.isInteger() : "need int for numeric op";
                assert typeOfRightOperand.isInteger() : "need int for numeric op";

                expression.getType().resolveToInteger();

                break;
            case LOGICAL_AND:
            case LOGICAL_OR:
                // Operands for logical operations must be integers.
                assert typeOfLeftOperand.isBoolean() : "need boolean for logic op";
                assert typeOfRightOperand.isBoolean() : "need boolean for logic op";

                expression.getType().resolveToBoolean();

                break;
            case LESS_THAN:
            case LESS_THAN_OR_EQUAL_TO:
            case GREATER_THAN:
            case GREATER_THAN_OR_EQUAL_TO:
                // Operands for numeric comparison must be integers.
                assert typeOfLeftOperand.isInteger() : "need int for comparison";
                assert typeOfRightOperand.isInteger() : "need int for comparison";

                expression.getType().resolveToBoolean();

                break;
            case EQUAL_TO:
            case NOT_EQUAL_TO:
                // Left and right operands must be comparable.
                assert canCheckForEqualityWithTypesOfExpressions(typeOfLeftOperand, typeOfRightOperand);

                expression.getType().resolveToBoolean();

                break;
            case ASSIGNMENT:
                // Left operand must be assignable.
                assert typeOfLeftOperand.isAssignable() : "not assignable";

                // Left and right operands must be compatible.
                assert canAssignTypeOfExpressionToTypeOfExpression(typeOfRightOperand, typeOfLeftOperand) :
                        "incompatible operands for assignment";

                expression.getType().resolveToTypeOfExpression(typeOfLeftOperand, false);

                break;
            default:
                throw new AssertionError();
        }
    }

    @Override
    protected void visit(Expression.UnaryOperation expression, Void context) {
        expression.getOther().accept(this, context);

        switch (expression.getOperationType()) {
            case LOGICAL_NEGATION:
                // Operand for logical negation must be boolean.
                assert expression.getType().isBoolean() : "expected boolean";

                expression.getType().resolveToBoolean();

                break;
            case NUMERIC_NEGATION:
                // Operand for numeric negation must be integer.
                assert expression.getType().isInteger() : "expected int";

                expression.getType().resolveToInteger();

                break;
            default:
                throw new AssertionError();
        }
    }

    @Override
    protected void visit(Expression.NullLiteral expression, Void context) {
        expression.getType().resolveToNull();
    }

    @Override
    protected void visit(Expression.BooleanLiteral expression, Void context) {
        expression.getType().resolveToBoolean();
    }

    @Override
    protected void visit(Expression.IntegerLiteral expression, Void context) {

        // Literal value must fit in signed 32 bit.
        try {
            Integer.parseInt(expression.getValue());
        }
        catch (NumberFormatException exception) {
            assert false : "integer too big";
        }

        expression.getType().resolveToInteger();
    }

    @Override
    protected void visit(Expression.MethodInvocation expression, Void context) {

        expression.getContext().ifPresent(node -> node.accept(this, context));
        expression.getArguments().forEach(node -> node.accept(this, context));

        String methodName = expression.getMethodReference().getName();
        Optional<MethodDeclaration> methodDeclaration;

        if (expression.getContext().isPresent()) {
            TypeOfExpression typeOfContext = expression.getContext().get().getType();

            // Methods cannot be invoked on null literal.
            assert typeOfContext.getDeclaration().isPresent() : "cannot invoke method on null";

            // Methods cannot be invoked on arrays.
            assert typeOfContext.getNumberOfDimensions() == 0 : "cannot invoke method on array";

            BasicTypeDeclaration typeDeclaration = typeOfContext.getDeclaration().get();

            // Methods can only be invoked on objects.
            assert typeDeclaration instanceof ClassDeclaration : "cannot invoke method on non-object";

            methodDeclaration = this.getMethodDeclarationForName(methodName, (ClassDeclaration)typeDeclaration);
        }
        else {
            // Must not invoke methods in static context.
            assert !(this.getCurrentMethodDeclaration() instanceof MainMethodDeclaration) : "cant access this";

            methodDeclaration = this.getMethodDeclarationForName(methodName, this.getCurrentClassDeclaration());
        }

        // Method reference must be resolvable.
        assert methodDeclaration.isPresent() : "use of undeclared method";

        List<TypeOfExpression> typesOfArguments = expression.getArgumentTypes();
        List<TypeReference> typesOfParameters = methodDeclaration.get().getParameterTypes();

        // Number of arguments must match.
        assert typesOfArguments.size() == typesOfParameters.size() : "incorrect number of arguments";

        for (int index = 0; index < typesOfArguments.size(); index += 1) {

            // Type of argument must be compatible.
            assert canAssignTypeOfExpressionToTypeReference(typesOfArguments.get(index), typesOfParameters.get(index)) :
                    "incompatible argument";
        }

        expression.getMethodReference().resolveTo(methodDeclaration.get());
        expression.getType().resolveToTypeReference(methodDeclaration.get().getReturnType(), false);
    }

    @Override
    protected void visit(Expression.ExplicitFieldAccess expression, Void context) {

        expression.getContext().accept(this, context);

        TypeOfExpression typeOfContext = expression.getContext().getType();

        // Fields cannot be accessed on null literal.
        assert typeOfContext.getDeclaration().isPresent() : "cannot access field on null";

        // Fields cannot be accessed on arrays.
        assert typeOfContext.getNumberOfDimensions() == 0 : "cannot access field on array";

        BasicTypeDeclaration typeDeclaration = typeOfContext.getDeclaration().get();

        // Fields can only be accessed on objects.
        assert typeDeclaration instanceof ClassDeclaration : "cannot access field on non-object";

        String fieldName = expression.getFieldReference().getName();
        ClassDeclaration classDeclaration = (ClassDeclaration)typeDeclaration;
        Optional<FieldDeclaration> fieldDeclaration = this.getFieldDeclarationForName(fieldName, classDeclaration);

        // Field reference must be resolvable.
        assert fieldDeclaration.isPresent() : "use of undeclared field";

        expression.getFieldReference().resolveTo(fieldDeclaration.get());
        expression.getType().resolveToTypeReference(fieldDeclaration.get().getType(), true);
    }

    @Override
    protected void visit(Expression.ArrayElementAccess expression, Void context) {
        expression.getContext().accept(this, context);
        expression.getIndex().accept(this, context);

        TypeOfExpression typeOfContext = expression.getContext().getType();

        // Context must not be null literal.
        assert typeOfContext.getDeclaration().isPresent() : "context of array access must not be null";

        // Context must be an array of some sort.
        assert typeOfContext.getNumberOfDimensions() >= 1 : "context of array access must be array";

        // Array index must be an integer.
        assert expression.getIndex().getType().isInteger() : "array index must be integer";

        BasicTypeDeclaration basicTypeDeclaration = typeOfContext.getDeclaration().get();
        int numberOfDimensions = typeOfContext.getNumberOfDimensions() - 1;

        expression.getType().resolveToArrayOf(basicTypeDeclaration, numberOfDimensions, true);
    }

    @Override
    protected void visit(Expression.VariableAccess expression, Void context) {
        String name = expression.getVariableReference().getName();
        Optional<VariableDeclaration> variableDeclaration = this.getVariableDeclarationForName(name);

        // Variable reference must be resolvable.
        assert variableDeclaration.isPresent() : "use of undeclared variable";

        // Variable must be accessible. arguments parameter for main method is not accessible.
        assert variableDeclaration.get().canBeAccessed() : "variable may not be accessed. sorry.";

        expression.getVariableReference().resolveTo(variableDeclaration.get());
        expression.getType().resolveToTypeReference(variableDeclaration.get().getType(), true);
    }

    @Override
    protected void visit(Expression.CurrentContextAccess expression, Void context) {

        // Current context may not be accessed in main methods.
        assert !(this.getCurrentMethodDeclaration() instanceof MainMethodDeclaration) : "must not access this";

        expression.getType().resolveToInstanceOfClass(this.getCurrentClassDeclaration(), false);
    }

    @Override
    protected void visit(Expression.NewObjectCreation expression, Void context) {
        String className = expression.getClassReference().getName();
        Optional<ClassDeclaration> classDeclaration = this.getClassDeclarationForName(className);

        // Class reference must ve resolvable.
        assert classDeclaration.isPresent() : "use of undeclared class";

        expression.getClassReference().resolveTo(classDeclaration.get());
        expression.getType().resolveToInstanceOfClass(classDeclaration.get(), false);
    }

    @Override
    protected void visit(Expression.NewArrayCreation expression, Void context) {
        expression.getPrimaryDimension().accept(this, context);

        // Primary dimension must be integer.
        assert expression.getPrimaryDimension().getType().isInteger() : "primary dimension must be integer";

        String name = expression.getBasicTypeReference().getName();
        Optional<BasicTypeDeclaration> basicTypeDeclaration = this.getBasicTypeDeclarationForName(name);

        // Basic type reference must be resolvable.
        assert basicTypeDeclaration.isPresent() : "use of undeclared type";

        // Must not be array of void.
        assert basicTypeDeclaration.get() != PrimitiveTypeDeclaration.VOID : "array of void is not allowed";

        expression.getBasicTypeReference().resolveTo(basicTypeDeclaration.get());
        expression.getType().resolveToArrayOf(basicTypeDeclaration.get(), expression.getNumberOfDimensions(), false);
    }
}
