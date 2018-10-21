package edu.kit.minijava.parser;

public final class IntegerLiteral extends Expression {
    public IntegerLiteral(int value) {
        this.value = value;
    }

    public final int value;

    @Override
    public String toString() {
        return "IntegerLiteral(" + this.value + ")";
    }
}
