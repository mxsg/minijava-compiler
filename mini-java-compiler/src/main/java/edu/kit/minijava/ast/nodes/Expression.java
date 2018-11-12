package edu.kit.minijava.ast.nodes;

import edu.kit.minijava.ast.references.*;
import edu.kit.minijava.lexer.*;

import java.util.*;
import java.util.stream.*;

public abstract class Expression implements ASTNode {
    private Expression() {
        this.type = new TypeOfExpression();
    }

    private final TypeOfExpression type;

    public TypeOfExpression getType() {
        return this.type;
    }

    public abstract boolean isValidForExpressionStatement();

    public static final class BinaryOperation extends Expression {
        public BinaryOperation(BinaryOperationType operationType, Expression left, Expression right) {
            super();

            this.operationType = operationType;
            this.left = left;
            this.right = right;
        }

        private final BinaryOperationType operationType;
        private final Expression left;
        private final Expression right;

        public BinaryOperationType getOperationType() {
            return this.operationType;
        }

        public Expression getLeft() {
            return this.left;
        }

        public Expression getRight() {
            return this.right;
        }

        @Override
        public boolean isValidForExpressionStatement() {
            return this.operationType == BinaryOperationType.ASSIGNMENT;
        }

        @Override
        public <T> void accept(ASTVisitor<T> visitor, T context) {
            visitor.visit(this, context);
        }
    }

    public static final class UnaryOperation extends Expression {
        public UnaryOperation(UnaryOperationType operationType, Expression other) {
            super();

            this.operationType = operationType;
            this.other = other;
        }

        private final UnaryOperationType operationType;
        private final Expression other;

        public UnaryOperationType getOperationType() {
            return this.operationType;
        }

        public Expression getOther() {
            return this.other;
        }

        @Override
        public boolean isValidForExpressionStatement() {
            return false;
        }

        @Override
        public <T> void accept(ASTVisitor<T> visitor, T context) {
            visitor.visit(this, context);
        }
    }

    public static final class NullLiteral extends Expression {
        public NullLiteral() {
            super();
        }

        @Override
        public boolean isValidForExpressionStatement() {
            return false;
        }

        @Override
        public <T> void accept(ASTVisitor<T> visitor, T context) {
            visitor.visit(this, context);
        }
    }

    public static final class BooleanLiteral extends Expression {
        public BooleanLiteral(boolean value) {
            super();

            this.value = value;
        }

        private final boolean value;

        public boolean getValue() {
            return this.value;
        }

        @Override
        public boolean isValidForExpressionStatement() {
            return false;
        }

        @Override
        public <T> void accept(ASTVisitor<T> visitor, T context) {
            visitor.visit(this, context);
        }
    }

    public static final class IntegerLiteral extends Expression {
        public IntegerLiteral(String value) {
            super();

            this.value = value;
        }

        private final String value;

        public String getValue() {
            return this.value;
        }

        @Override
        public boolean isValidForExpressionStatement() {
            return false;
        }

        @Override
        public <T> void accept(ASTVisitor<T> visitor, T context) {
            visitor.visit(this, context);
        }
    }

    public static final class MethodInvocation extends Expression {
        public MethodInvocation(String methodName, List<Expression> arguments, TokenLocation location) {
            super();

            List<TypeOfExpression> argumentTypes = arguments.stream().map(e -> e.type).collect(Collectors.toList());

            this.context = null;
            this.arguments = arguments;
            this.methodReference = new MethodReference(methodName, argumentTypes, location);
        }

        public MethodInvocation(Expression context, String methodName, List<Expression> arguments,
                                TokenLocation location) {
            super();

            List<TypeOfExpression> argumentTypes = arguments.stream().map(e -> e.type).collect(Collectors.toList());

            this.context = context;
            this.arguments = arguments;
            this.methodReference = new MethodReference(context.type, methodName, argumentTypes, location);
        }

        private final Expression context; // nullable
        private final MethodReference methodReference;
        private final List<Expression> arguments;

        public Optional<Expression> getContext() {
            return Optional.ofNullable(this.context);
        }

        public MethodReference getMethodReference() {
            return this.methodReference;
        }

        public List<Expression> getArguments() {
            return this.arguments;
        }

        @Override
        public boolean isValidForExpressionStatement() {
            return true;
        }

        @Override
        public <T> void accept(ASTVisitor<T> visitor, T context) {
            visitor.visit(this, context);
        }
    }

    public static final class ExplicitFieldAccess extends Expression {
        public ExplicitFieldAccess(Expression context, String fieldName, TokenLocation location) {
            super();

            this.context = context;
            this.fieldReference = new FieldReference(context.type, fieldName, location);
        }

        private final Expression context;
        private final FieldReference fieldReference;

        public Expression getContext() {
            return this.context;
        }

        public FieldReference getFieldReference() {
            return this.fieldReference;
        }

        @Override
        public boolean isValidForExpressionStatement() {
            return false;
        }

        @Override
        public <T> void accept(ASTVisitor<T> visitor, T context) {
            visitor.visit(this, context);
        }
    }

    public static final class ArrayElementAccess extends Expression {
        public ArrayElementAccess(Expression context, Expression index) {
            super();

            this.context = context;
            this.index = index;
        }

        private final Expression context;
        private final Expression index;

        public Expression getContext() {
            return this.context;
        }

        public Expression getIndex() {
            return this.index;
        }

        @Override
        public boolean isValidForExpressionStatement() {
            return false;
        }

        @Override
        public <T> void accept(ASTVisitor<T> visitor, T context) {
            visitor.visit(this, context);
        }
    }

    public static final class VariableAccess extends Expression {
        public VariableAccess(String variableName, TokenLocation location) {
            super();

            this.variableReference = new VariableReference(variableName, location);
        }

        private final VariableReference variableReference;

        public VariableReference getVariableReference() {
            return this.variableReference;
        }

        @Override
        public boolean isValidForExpressionStatement() {
            return false;
        }

        @Override
        public <T> void accept(ASTVisitor<T> visitor, T context) {
            visitor.visit(this, context);
        }
    }

    public static final class CurrentContextAccess extends Expression {
        public CurrentContextAccess() {
            super();
        }

        @Override
        public boolean isValidForExpressionStatement() {
            return false;
        }

        @Override
        public <T> void accept(ASTVisitor<T> visitor, T context) {
            visitor.visit(this, context);
        }
    }

    public static final class NewObjectCreation extends Expression {
        public NewObjectCreation(String className, TokenLocation location) {
            super();

            this.classReference = new ClassReference(className, location);
        }

        private final ClassReference classReference;

        public ClassReference getClassReference() {
            return this.classReference;
        }

        @Override
        public boolean isValidForExpressionStatement() {
            return false;
        }

        @Override
        public <T> void accept(ASTVisitor<T> visitor, T context) {
            visitor.visit(this, context);
        }
    }

    public static final class NewArrayCreation extends Expression {
        public NewArrayCreation(BasicTypeReference basicTypeReference, Expression primaryDimension,
                                int numberOfDimensions) {
            super();

            this.basicTypeReference = basicTypeReference;
            this.primaryDimension = primaryDimension;
            this.numberOfDimensions = numberOfDimensions;
        }

        private final BasicTypeReference basicTypeReference;
        private final Expression primaryDimension;
        private final int numberOfDimensions;

        public BasicTypeReference getBasicTypeReference() {
            return this.basicTypeReference;
        }

        public Expression getPrimaryDimension() {
            return this.primaryDimension;
        }

        public int getNumberOfDimensions() {
            return this.numberOfDimensions;
        }

        @Override
        public boolean isValidForExpressionStatement() {
            return false;
        }

        @Override
        public <T> void accept(ASTVisitor<T> visitor, T context) {
            visitor.visit(this, context);
        }
    }
}
