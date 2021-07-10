/**
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.lang.apex.rule.performance;

import net.sourceforge.pmd.lang.apex.ast.ASTDmlDeleteStatement;
import net.sourceforge.pmd.lang.apex.ast.ASTDmlInsertStatement;
import net.sourceforge.pmd.lang.apex.ast.ASTDmlMergeStatement;
import net.sourceforge.pmd.lang.apex.ast.ASTDmlUndeleteStatement;
import net.sourceforge.pmd.lang.apex.ast.ASTDmlUpdateStatement;
import net.sourceforge.pmd.lang.apex.ast.ASTDmlUpsertStatement;
import net.sourceforge.pmd.lang.apex.ast.ASTMethodCallExpression;
import net.sourceforge.pmd.lang.apex.ast.ASTRunAsBlockStatement;
import net.sourceforge.pmd.lang.apex.ast.ASTSoqlExpression;
import net.sourceforge.pmd.lang.apex.ast.ASTSoslExpression;
import net.sourceforge.pmd.lang.apex.rule.internal.Helper;

/**
 * Warn users when code that could trigger governor limits is executing within a looping construct.
 */
public class OperationWithLimitsInLoopRule extends AbstractAvoidNodeInLoopsRule {

    private static final String APPROVAL_CLASS_NAME = "Approval";
    private static final String MESSAGING_CLASS_NAME = "Messaging";
    private static final String SYSTEM_CLASS_NAME = "System";

    private static final String[] MESSAGING_LIMIT_METHODS = new String[] { "renderEmailTemplate", "renderStoredEmailTemplate", "sendEmail" };
    private static final String[] SYSTEM_LIMIT_METHODS = new String[] { "enqueueJob", "schedule", "scheduleBatch" };

    public OperationWithLimitsInLoopRule() {
        setProperty(CODECLIMATE_CATEGORIES, "Performance");
        // Note: Often more complicated as just moving a few lines.
        // Involves Maps...
        setProperty(CODECLIMATE_REMEDIATION_MULTIPLIER, 150);
        setProperty(CODECLIMATE_BLOCK_HIGHLIGHTING, false);

        // DML
        addRuleChainVisit(ASTDmlDeleteStatement.class);
        addRuleChainVisit(ASTDmlInsertStatement.class);
        addRuleChainVisit(ASTDmlMergeStatement.class);
        addRuleChainVisit(ASTDmlUndeleteStatement.class);
        addRuleChainVisit(ASTDmlUpdateStatement.class);
        addRuleChainVisit(ASTDmlUpsertStatement.class);
        // SOQL
        addRuleChainVisit(ASTSoqlExpression.class);
        // SOSL
        addRuleChainVisit(ASTSoslExpression.class);
        // Other limit consuming methods
        addRuleChainVisit(ASTRunAsBlockStatement.class);
        addRuleChainVisit(ASTMethodCallExpression.class);
    }

    // Begin DML Statements
    @Override
    public Object visit(ASTDmlDeleteStatement node, Object data) {
        return checkForViolation(node, data);
    }

    @Override
    public Object visit(ASTDmlInsertStatement node, Object data) {
        return checkForViolation(node, data);
    }

    @Override
    public Object visit(ASTDmlMergeStatement node, Object data) {
        return checkForViolation(node, data);
    }

    @Override
    public Object visit(ASTDmlUndeleteStatement node, Object data) {
        return checkForViolation(node, data);
    }

    @Override
    public Object visit(ASTDmlUpdateStatement node, Object data) {
        return checkForViolation(node, data);
    }

    @Override
    public Object visit(ASTDmlUpsertStatement node, Object data) {
        return checkForViolation(node, data);
    }
    // End DML Statements

    // Begin SOQL method invocations
    @Override
    public Object visit(ASTSoqlExpression node, Object data) {
        return checkForViolation(node, data);
    }
    // End SOQL method invocations

    // Begin SOSL method invocations
    @Override
    public Object visit(ASTSoslExpression node, Object data) {
        return checkForViolation(node, data);
    }
    // End SOSL method invocations

    // Begin general method invocations

    @Override
    public Object visit(ASTRunAsBlockStatement node, Object data) {
        return checkForViolation(node, data);
    }

    @Override
    public Object visit(ASTMethodCallExpression node, Object data) {
        if (Helper.isAnyDatabaseMethodCall(node) 
            || Helper.isMethodName(node, APPROVAL_CLASS_NAME, Helper.ANY_METHOD)
            || checkLimitClassMethods(node, MESSAGING_CLASS_NAME, MESSAGING_LIMIT_METHODS)
            || checkLimitClassMethods(node, SYSTEM_CLASS_NAME, SYSTEM_LIMIT_METHODS)) {

            return checkForViolation(node, data);
        } else {
            return data;
        }
    }

    private boolean checkLimitClassMethods(ASTMethodCallExpression node, String className, String[] methodNames) {

        for (String method : methodNames) {
            if (Helper.isMethodName(node, className, method)) {
                return true;
            }
        }

        return false;
    }
    // End general method invocations
}
