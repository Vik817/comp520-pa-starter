package miniJava.SyntacticAnalyzer;

import miniJava.ErrorReporter;
import miniJava.AbstractSyntaxTrees.Package;
import miniJava.AbstractSyntaxTrees.*;

import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;

public class Parser {
    private final Scanner _scanner;
    private final ErrorReporter _errors;
    private Token _currentToken;

    public FieldDeclList fieldList = new FieldDeclList();
    public MethodDeclList methodList = new MethodDeclList();
    public ClassDeclList classList = new ClassDeclList();
    public Parser(Scanner scanner, ErrorReporter errors) {
        this._scanner = scanner;
        this._errors = errors;
    }

    public Package parse() {
        _currentToken = _scanner.scan();
        try {
            // The first thing we need to parse is the Program symbol
            return parseProgram();
        } catch (SyntaxError e) {
            return null;
        }
    }

    // Program ::= (ClassDeclaration)* eot
    private Package parseProgram() throws SyntaxError {
        // TODO: Keep parsing class declarations until eot
        classList = new ClassDeclList();
        while (_currentToken.getTokenType() != TokenType.EOT) {
            classList.add(parseClassDeclaration());
        }
        accept(TokenType.EOT);
        return new Package(classList, null);
    }

    // ClassDeclaration ::= class identifier { (FieldDeclaration|MethodDeclaration)* }
    private ClassDecl parseClassDeclaration() throws SyntaxError {
        // TODO: Take in a "class" token (check by the TokenType)
        accept(TokenType.CLASS);
        //  What should be done if the first token isn't "class"?

        // TODO: Take in an identifier token
        String className = _currentToken.getTokenText();
        accept(TokenType.ID);
        // TODO: Take in a {
        accept(TokenType.LCURL);
        // TODO: Parse either a FieldDeclaration or MethodDeclaration
        while (_currentToken.getTokenType() != TokenType.RCURL) {
            parseFieldDeclaration();
        }
        // TODO: Take in a }
        accept(TokenType.RCURL);
        ClassDecl classdecl = new ClassDecl(className, fieldList, methodList, null);
        fieldList = new FieldDeclList();
        methodList = new MethodDeclList();
        return classdecl;
    }

    private FieldDecl parseFieldDeclaration() throws SyntaxError {
        FieldDecl fieldD;
        boolean isPriv = parseVisibility();
        boolean isStatic = parseAccess();
        if (_currentToken.getTokenType() == TokenType.VOID) {
            accept(TokenType.VOID);
            String name = _currentToken.getTokenText();
            accept(TokenType.ID);
            //Create a fieldDecl and use it as a parameter in making a MethodDecl
            fieldD = new FieldDecl(isPriv, isStatic, new BaseType(TypeKind.VOID, null), name, null);
            methodList.add(parseMethodDeclaration(fieldD));
            return null;
        } else {
            TypeDenoter t = parseType();
            String name = _currentToken.getTokenText();
            accept(TokenType.ID);
            fieldD = new FieldDecl(isPriv, isStatic, t, name, null);
            if (_currentToken.getTokenType() == TokenType.LPAREN) {
                methodList.add(parseMethodDeclaration(fieldD));
                return null;
            } else {
                accept(TokenType.SEMICOLON);
            }
            //Add to FieldDeclList
            fieldList.add(fieldD);
            return fieldD;
        }
    }

    private MethodDecl parseMethodDeclaration(MemberDecl mem) throws SyntaxError {
        accept(TokenType.LPAREN);
        ParameterDeclList parList = new ParameterDeclList();
        StatementList stateList = new StatementList();
        if (_currentToken.getTokenType() != TokenType.RPAREN) {
            parList = parseParameterList();
        }
        accept(TokenType.RPAREN);
        accept(TokenType.LCURL);
        while (_currentToken.getTokenType() != TokenType.RCURL) {
            stateList.add(parseStatement());
        }
        accept(TokenType.RCURL);
        return new MethodDecl(mem, parList, stateList, null);
    }

    private boolean parseVisibility() throws SyntaxError {
        if (_currentToken.getTokenType() == TokenType.VISIBILITY) {
            if(_currentToken.getTokenText().equals("private")) {
                accept(TokenType.VISIBILITY);
                return true;
            }
            accept(TokenType.VISIBILITY);
        }
        return false;
    }

    private boolean parseAccess() throws SyntaxError {
        if (_currentToken.getTokenType() == TokenType.STATIC) {
            accept(TokenType.STATIC);
            return true;
        }
        return false;
    }

    // NEED TO FIX PARSETYPE
    private TypeDenoter parseType() throws SyntaxError {
        TokenType type = _currentToken.getTokenType();
        TypeDenoter tD;
        if (type == TokenType.BOOLEAN) {
            accept(TokenType.BOOLEAN);
            tD = new BaseType(TypeKind.BOOLEAN, null);
            return tD;
        } else if (type == TokenType.INT) {
            accept(TokenType.INT);
            if (_currentToken.getTokenType() == TokenType.LBRACK) {
                accept(TokenType.LBRACK);
                accept(TokenType.RBRACK);
                tD = new ArrayType(new BaseType(TypeKind.INT, null), null);
                return tD;
            }
            tD = new BaseType(TypeKind.INT, null);
            return tD;
        } else if (type == TokenType.ID) {
            Identifier i = new Identifier(_currentToken);
            accept(TokenType.ID);
            if (_currentToken.getTokenType() == TokenType.LBRACK) {
                accept(TokenType.LBRACK);
                accept(TokenType.RBRACK);
                tD = new ArrayType(new ClassType(i, null), null);
                return tD;
            }
            tD = new ClassType(i, null);
            return tD;
        }
        return null;
    }

    private ParameterDeclList parseParameterList() throws SyntaxError {
        ParameterDeclList pList = new ParameterDeclList();
        TypeDenoter t = parseType();
        String name = _currentToken.getTokenText();
        accept(TokenType.ID);
        pList.add(new ParameterDecl(t, name, null));
        while (_currentToken.getTokenType() == TokenType.COMMA) {
            accept(TokenType.COMMA);
            t = parseType();
            name = _currentToken.getTokenText();
            accept(TokenType.ID);
            pList.add(new ParameterDecl(t, name, null));
        }
        return pList;
    }

    private ExprList parseArgumentList() throws SyntaxError {
        ExprList argList = new ExprList();
        Expression e = parseOr();
        argList.add(e);
        while (_currentToken.getTokenType() == TokenType.COMMA) {
            accept(TokenType.COMMA);
            e = parseOr();
            argList.add(e);
        }
        return argList;
    }

    private Reference parseReference() throws SyntaxError {
        Reference r = null;
        if (_currentToken.getTokenType() == TokenType.ID) {
            Identifier i = new Identifier(_currentToken);
            accept(TokenType.ID);
            r = new IdRef(i, null);
            if(_currentToken.getTokenType() != TokenType.DOT) {
                return r;
            }
        } else if (_currentToken.getTokenType() == TokenType.THIS) {
            accept(TokenType.THIS);
            r = new ThisRef(null);
            if(_currentToken.getTokenType() != TokenType.DOT) {
                return r;
            }
        }
        while (_currentToken.getTokenType() == TokenType.DOT) {
            accept(TokenType.DOT);
            r = new QualRef(r, new Identifier(_currentToken), null);
            accept(TokenType.ID);
        }
        return r;
        

    }

    private Statement parseStatement() throws SyntaxError {
        TokenType type = _currentToken.getTokenType();
        if (type == TokenType.LCURL) { // Statement  ::= { Statement* }
            StatementList sList = new StatementList();
            accept(TokenType.LCURL);
            while (_currentToken.getTokenType() != TokenType.RCURL) {
                sList.add(parseStatement());
            }
            accept(TokenType.RCURL);
            return new BlockStmt(sList, null);
        } else if (type == TokenType.BOOLEAN || type == TokenType.INT) { // ::= Type (BOOL or INT) id = Expression
            TypeDenoter t = parseType();
            String name = _currentToken.getTokenText();
            accept(TokenType.ID);
            accept(TokenType.ONEEQUAL);
            Expression e = parseOr();
            accept(TokenType.SEMICOLON);
            VarDecl vD = new VarDecl(t, name, null);
            return new VarDeclStmt(vD, e, null);
        } else if (type == TokenType.THIS) { // Reference (THIS)
            Reference r = parseReference();
            if (_currentToken.getTokenType() == TokenType.ONEEQUAL) { // ::= Reference = Expression;
                accept(TokenType.ONEEQUAL);
                Expression e = parseOr();
                accept(TokenType.SEMICOLON);
                return new AssignStmt(r, e, null);
            } else if (_currentToken.getTokenType() == TokenType.LBRACK) { // ::= Reference [Expression] = Expression;
                accept(TokenType.LBRACK);
                Expression first = parseOr();
                accept(TokenType.RBRACK);
                accept(TokenType.ONEEQUAL);
                Expression second = parseOr();
                accept(TokenType.SEMICOLON);
                return new IxAssignStmt(r, first, second, null);
            } else if (_currentToken.getTokenType() == TokenType.LPAREN) { // ::= Reference (ArgumentList?);
                ExprList aList = new ExprList();
                accept(TokenType.LPAREN);
                //Note, Changed "while" to "if" here, test if it still works later
                if (_currentToken.getTokenType() != TokenType.RPAREN) {
                    aList = parseArgumentList();
                }
                accept(TokenType.RPAREN);
                accept(TokenType.SEMICOLON);
                return new CallStmt(r, aList, null);
            }
        } else if (type == TokenType.ID) { // Differentiating between Type and Reference
            //If it is a reference, we will have aR be the reference itself so it can be used later
            Reference aR = new IdRef(new Identifier(_currentToken), null);
            //If it is a type, we will have theID be the className itself to be used later
            Identifier theID = new Identifier(_currentToken);
            accept(TokenType.ID);

            if (_currentToken.getTokenType() == TokenType.ID) { //Know it is Type, VarDeclStmt
                String name = _currentToken.getTokenText();
                accept(TokenType.ID);
                accept(TokenType.ONEEQUAL);
                Expression e = parseOr();
                accept(TokenType.SEMICOLON);
                VarDecl vD = new VarDecl(new ClassType(theID, null), name, null);
                return new VarDeclStmt(vD, e, null);
            } else if (_currentToken.getTokenType() == TokenType.LBRACK) { // Checking if Type or Ref
                accept(TokenType.LBRACK);
                if (_currentToken.getTokenType() == TokenType.RBRACK) { //Know this is a Type, VarDeclStmt
                    accept(TokenType.RBRACK);
                    String name = _currentToken.getTokenText();
                    accept(TokenType.ID);
                    accept(TokenType.ONEEQUAL);
                    Expression e = parseOr();
                    accept(TokenType.SEMICOLON);
                    VarDecl vD = new VarDecl(new ArrayType(new ClassType(theID, null), null), name, null);
                    return new VarDeclStmt(vD, e, null);
                } else { // Reference [ Expression ] = Expression; IxAssignStmt
                    Expression first = parseOr();
                    accept(TokenType.RBRACK);
                    accept(TokenType.ONEEQUAL);
                    Expression second = parseOr();
                    accept(TokenType.SEMICOLON);
                    return new IxAssignStmt(aR, first, second, null);
                }
            } else { // Reference 100%, use aR as the reference
                if (_currentToken.getTokenType() == TokenType.DOT) {
                    Identifier a = null;
                    while(_currentToken.getTokenType() == TokenType.DOT) {
                        accept(TokenType.DOT);
                        aR = new QualRef(aR, new Identifier(_currentToken), null);
                        accept(TokenType.ID);
                    }
                    //aR = new QualRef(aR, a, null); //Redefine aR to be a QualRef
                }
                if (_currentToken.getTokenType() == TokenType.ONEEQUAL) { // Reference = Expression;
                    accept(TokenType.ONEEQUAL);
                    Expression e = parseOr();
                    accept(TokenType.SEMICOLON);
                    return new AssignStmt(aR, e, null);
                } else if (_currentToken.getTokenType() == TokenType.LBRACK) { // Reference [Expression] = Expression;
                    accept(TokenType.LBRACK);
                    Expression i = parseOr();
                    accept(TokenType.RBRACK);
                    accept(TokenType.ONEEQUAL);
                    Expression e = parseOr();
                    accept(TokenType.SEMICOLON);
                    return new IxAssignStmt(aR, i, e, null);
                } else if (_currentToken.getTokenType() == TokenType.LPAREN) { // Reference (ArgumentList?);
                    ExprList aList = new ExprList();
                    accept(TokenType.LPAREN);
                    //Note, Changed "while" to "if" here, test if it still works later
                    if (_currentToken.getTokenType() != TokenType.RPAREN) {
                        aList = parseArgumentList();
                    }
                    accept(TokenType.RPAREN);
                    accept(TokenType.SEMICOLON);
                    return new CallStmt(aR, aList, null);
                }
            }
        } else if (type == TokenType.RETURN) { // return Expression? ;
            accept(TokenType.RETURN);
            Expression e = null;
            if (_currentToken.getTokenType() != TokenType.SEMICOLON) {
                e = parseOr();
            }
            accept(TokenType.SEMICOLON);
            return new ReturnStmt(e, null);
        } else if (type == TokenType.IF) { // if (Expression) Statement (else Statement)?
            accept(TokenType.IF);
            accept(TokenType.LPAREN);
            Expression b = parseOr();
            accept(TokenType.RPAREN);
            Statement t = parseStatement();
            if (_currentToken.getTokenType() == TokenType.ELSE) {
                accept(TokenType.ELSE);
                Statement e = parseStatement();
                return new IfStmt(b, t, e, null);
            }
            return new IfStmt(b, t, null);
        } else { //while (Expression) Statement
            accept(TokenType.WHILE);
            accept(TokenType.LPAREN);
            Expression e = parseOr();
            accept(TokenType.RPAREN);
            Statement t = parseStatement();
            return new WhileStmt(e, t, null);
        }
        return null;
    }

    private Expression parseExpression() throws SyntaxError {
        //Should check for an OPERATOR at the end of each one, to see if we have Expression binop Expression
        TokenType theType = _currentToken.getTokenType();
        if (theType == TokenType.LPAREN) { // ( Expression )
            accept(TokenType.LPAREN);
            Expression e = parseOr();
            accept(TokenType.RPAREN);
            return e;
        } else if (theType == TokenType.NUM) {
            IntLiteral myInt = new IntLiteral(_currentToken);
            accept(TokenType.NUM);
            return new LiteralExpr(myInt, null);
        } else if (theType == TokenType.TRUEFALSE) {
            BooleanLiteral myBool = new BooleanLiteral(_currentToken);
            accept(TokenType.TRUEFALSE);
            return new LiteralExpr(myBool, null);
        } else if (theType == TokenType.NEW) {
            accept(TokenType.NEW);
            if (_currentToken.getTokenType() == TokenType.ID) {
                Identifier ident = new Identifier(_currentToken);
                ClassType ct = new ClassType(ident, null);
                accept(TokenType.ID);
                if (_currentToken.getTokenType() == TokenType.LBRACK) { // new id[Expression]
                    accept(TokenType.LBRACK);
                    Expression e = parseOr();
                    accept(TokenType.RBRACK);
                    return  new NewArrayExpr(ct, e, null);
                } else { // new id()
                    accept(TokenType.LPAREN);
                    accept(TokenType.RPAREN);
                    return new NewObjectExpr(ct, null);
                }
            } else if (_currentToken.getTokenType() == TokenType.INT) { // new int[Expression]
                BaseType type = new BaseType(TypeKind.INT, null);
                accept(TokenType.INT);
                accept(TokenType.LBRACK);
                Expression e = parseOr();
                accept(TokenType.RBRACK);
                return new NewArrayExpr(type, e, null);
            } else {
                accept(TokenType.NUM);
            }
        } else if (theType == TokenType.ID || theType == TokenType.THIS) { // Reference
            Reference ref = parseReference();
            if (_currentToken.getTokenType() == TokenType.LBRACK) { // Reference [Expression]
                accept(TokenType.LBRACK);
                Expression e = parseOr();
                accept(TokenType.RBRACK);
                return new IxExpr(ref, e, null);
            } else if (_currentToken.getTokenType() == TokenType.LPAREN) { // Reference (ArgumentList?)
                ExprList list = new ExprList();
                accept(TokenType.LPAREN);
                //Changed while to if
                if (_currentToken.getTokenType() != TokenType.RPAREN) {
                    list = parseArgumentList();
                }
                accept(TokenType.RPAREN);
                return new CallExpr(ref, list, null);
            } else { //Just Reference
                return new RefExpr(ref, null);
            }
        } else if(theType == TokenType.NULL) {
            NullLiteral nLit = new NullLiteral(_currentToken);
            accept(TokenType.NULL);
            return new LiteralExpr(nLit, null);
        } else {
            accept(TokenType.NUM);
        }
        throw new SyntaxError();
    }

    private Expression parseOr() {
        Expression exp = parseAnd();
        while(_currentToken.getTokenText().equals("||")) {
            Operator op = new Operator(_currentToken);
            accept(TokenType.OPERATOR);
            Expression e2 = parseAnd();
            exp = new BinaryExpr(op, exp, e2, null);
        }
        return exp;
    }
    private Expression parseAnd() {
        Expression exp = parseEquals();
        while(_currentToken.getTokenText().equals("&&")) {
            Operator op = new Operator(_currentToken);
            accept(TokenType.OPERATOR);
            Expression e2 = parseEquals();
            exp = new BinaryExpr(op, exp, e2, null);
        }
        return exp;
    }
    private Expression parseEquals() {
        Expression exp = parseRelational();
        while(_currentToken.getTokenText().equals("==") || _currentToken.getTokenText().equals("!=")) {
            Operator op = new Operator(_currentToken);
            accept(TokenType.OPERATOR);
            Expression e2 = parseRelational();
            exp = new BinaryExpr(op, exp, e2, null);
        }
        return exp;
    }
    private Expression parseRelational() {
        Expression exp = parseAdditive();
        while(_currentToken.getTokenText().equals("<=") || _currentToken.getTokenText().equals(">=")
                || _currentToken.getTokenText().equals("<") || _currentToken.getTokenText().equals(">")) {
            Operator op = new Operator(_currentToken);
            accept(TokenType.OPERATOR);
            Expression e2 = parseAdditive();
            exp = new BinaryExpr(op, exp, e2, null);
        }
        return exp;
    }
    private Expression parseAdditive() {
        Expression exp = parseMultiplicative();
        while(_currentToken.getTokenText().equals("+") || _currentToken.getTokenText().equals("-")) {
            Operator op = new Operator(_currentToken);
            if(_currentToken.getTokenType() == TokenType.NEGATIVE) {
                accept(TokenType.NEGATIVE);
            } else {
                accept(TokenType.OPERATOR);
            }
            Expression e2 = parseMultiplicative();
            exp = new BinaryExpr(op, exp, e2, null);
        }
        return exp;
    }
    private Expression parseMultiplicative() {
        Expression exp = parseUnary();
        while(_currentToken.getTokenText().equals("*") || _currentToken.getTokenText().equals("/")) {
            Operator op = new Operator(_currentToken);
            accept(TokenType.OPERATOR);
            Expression e2 = parseUnary();
            exp = new BinaryExpr(op, exp, e2, null);
        }
        return exp;
    }
    private Expression parseUnary() {
        Expression exp;
        if(_currentToken.getTokenText().equals("-") || _currentToken.getTokenText().equals("!")) {
            Operator op = new Operator(_currentToken);
            if(_currentToken.getTokenType() == TokenType.NEGATIVE) {
                accept(TokenType.NEGATIVE);
            } else {
                accept(TokenType.EXCLAMATION);
            }
            Expression e = parseUnary();
            exp = new UnaryExpr(op, e, null);
        } else {
            return parseExpression();
        }
        return exp;
    }

    // This method will accept the token and retrieve the next token.
    //  Can be useful if you want to error check and accept all-in-one.
    private void accept(TokenType expectedType) throws SyntaxError {
        if (_currentToken.getTokenType() == expectedType) {
            _currentToken = _scanner.scan();
            //System.out.println(_currentToken.getTokenText());
            return;
        }
        // TODO: Report an error here.
        //  "Expected token X, but got Y"
        _errors.reportError("Expected: " + expectedType +
                " but got " + _currentToken.getTokenType());
        throw new SyntaxError();
    }

    class SyntaxError extends Error {
        private static final long serialVersionUID = -6461942006097999362L;
    }
}
