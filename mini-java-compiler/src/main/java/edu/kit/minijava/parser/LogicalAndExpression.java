package edu.kit.minijava.parser;

public final class LogicalAndExpression extends Expression {
    public LogicalAndExpression(Expression left, Expression right) {
        this.left = left;
        this.right = right;
    }

    public final Expression left;
    public final Expression right;
}
