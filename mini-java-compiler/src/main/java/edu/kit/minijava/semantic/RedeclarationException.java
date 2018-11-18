package edu.kit.minijava.semantic;

import edu.kit.minijava.ast.nodes.Declaration;
import edu.kit.minijava.lexer.TokenLocation;

public class RedeclarationException extends SemanticException {
    public RedeclarationException(String identifier, TokenLocation location, Declaration previousDeclaration) {
        if (identifier == null) throw new IllegalArgumentException();
        if (previousDeclaration == null) throw new IllegalArgumentException();

        this.identifier = identifier;
        this.location = location;
        this.previousDeclaration = previousDeclaration;
    }

    private String identifier;
    private TokenLocation location; // nullable
    private Declaration previousDeclaration;

    public String getIdentifier() {
        return this.identifier;
    }

    public TokenLocation getLocation() {
        return this.location;
    }

    public Declaration getPreviousDeclaration() {
        return this.previousDeclaration;
    }

    @Override
    public String getMessage() {
        StringBuilder builder = new StringBuilder();

        builder.append("Invalid redeclaration of identifier ");
        builder.append(this.identifier);
        builder.append(" ");

        if (this.location != null) {
            builder.append("at ");
            builder.append(this.location);
        }

        builder.append(" (previous declaration: ");
        builder.append(this.previousDeclaration);
        builder.append(").");

        return builder.toString();
    }
}
