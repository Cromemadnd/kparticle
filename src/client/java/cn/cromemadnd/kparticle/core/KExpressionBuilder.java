package cn.cromemadnd.kparticle.core;

import net.objecthunter.exp4j.ExpressionBuilder;

public class KExpressionBuilder extends ExpressionBuilder {
    public KExpressionBuilder(String expression) {
        super(expression);
        this.functions(
            KMathFuncs.max,
            KMathFuncs.min,
            KMathFuncs.clamp,
            KMathFuncs.random,
            KMathFuncs._if,
            KMathFuncs.sind,
            KMathFuncs.cosd,
            KMathFuncs.tand
        ).variables("t", "p", "c", "n");
    }
}
