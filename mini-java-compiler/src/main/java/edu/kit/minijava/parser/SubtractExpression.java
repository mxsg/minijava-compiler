package edu.kit.minijava.parser;

import util.INodeVisitor;

public final class SubtractExpression extends Expression {
    public SubtractExpression(Expression left, Expression right) {
        if (left == null) throw new IllegalArgumentException();
        if (right == null) throw new IllegalArgumentException();

        this.left = left;
        this.right = right;
    }

    public final Expression left;
    public final Expression right;

    @Override
    public String toString() {
        return "SubtractExpression(" + this.left + ", " + this.right + ")";
    }
    
    @Override
    public void accept(INodeVisitor visitor) {
        visitor.visit(this);
    }
}
